using System.Collections.Concurrent;
using System.Security.Cryptography;
using System.Text;
using Vantafyn.Plugin.Companion.Core;

namespace Vantafyn.Plugin.Companion.Requests;

public interface IOmbiUserSessionStore
{
    Task<OmbiLinkedUserSession?> ReadAsync(Guid jellyfinUserId, CancellationToken cancellationToken);
    Task SaveAsync(Guid jellyfinUserId, OmbiLinkedUserSession session, string accessToken, CancellationToken cancellationToken);
    Task DeleteAsync(Guid jellyfinUserId, CancellationToken cancellationToken);
}

public sealed class FileOmbiUserSessionStore(ICompanionPaths paths, IClock clock) : IOmbiUserSessionStore
{
    private static readonly ConcurrentDictionary<Guid, SemaphoreSlim> Locks = new();
    private const int KeyBytes = 32;
    private const int NonceBytes = 12;

    public async Task<OmbiLinkedUserSession?> ReadAsync(Guid jellyfinUserId, CancellationToken cancellationToken)
    {
        await LockFor(jellyfinUserId).WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var envelope = await JsonFile.ReadAsync<OmbiUserSessionEnvelope>(PathFor(jellyfinUserId), cancellationToken).ConfigureAwait(false);
            if (envelope == null) return null;
            var token = Decrypt(envelope.EncryptedAccessToken);
            return new OmbiLinkedUserSession(
                envelope.OmbiUserName,
                envelope.DisplayName,
                envelope.OmbiUserId,
                envelope.ExpiresAt,
                envelope.Roles,
                envelope.LastLoginAt,
                envelope.LastValidatedAt,
                token);
        }
        finally
        {
            LockFor(jellyfinUserId).Release();
        }
    }

    public async Task SaveAsync(Guid jellyfinUserId, OmbiLinkedUserSession session, string accessToken, CancellationToken cancellationToken)
    {
        await LockFor(jellyfinUserId).WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var envelope = new OmbiUserSessionEnvelope(
                1,
                session.OmbiUserName,
                session.DisplayName,
                session.OmbiUserId,
                session.ExpiresAt,
                session.Roles,
                session.LastLoginAt,
                clock.UtcNow,
                Encrypt(accessToken));
            await JsonFile.WriteAtomicAsync(PathFor(jellyfinUserId), envelope, cancellationToken).ConfigureAwait(false);
        }
        finally
        {
            LockFor(jellyfinUserId).Release();
        }
    }

    public async Task DeleteAsync(Guid jellyfinUserId, CancellationToken cancellationToken)
    {
        await LockFor(jellyfinUserId).WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var path = PathFor(jellyfinUserId);
            if (File.Exists(path))
            {
                File.Delete(path);
            }
        }
        finally
        {
            LockFor(jellyfinUserId).Release();
        }
    }

    private string PathFor(Guid userId) => Path.Combine(paths.OmbiSessionsRoot, userId.ToString("N") + ".json");
    private static SemaphoreSlim LockFor(Guid userId) => Locks.GetOrAdd(userId, _ => new SemaphoreSlim(1, 1));

    private EncryptedValue Encrypt(string value)
    {
        var nonce = RandomNumberGenerator.GetBytes(NonceBytes);
        var plaintext = Encoding.UTF8.GetBytes(value);
        var ciphertext = new byte[plaintext.Length];
        var tag = new byte[16];
        using var aes = new AesGcm(ReadOrCreateKey(), tag.Length);
        aes.Encrypt(nonce, plaintext, ciphertext, tag);
        return new EncryptedValue(
            Convert.ToBase64String(nonce),
            Convert.ToBase64String(ciphertext),
            Convert.ToBase64String(tag));
    }

    private string Decrypt(EncryptedValue value)
    {
        var nonce = Convert.FromBase64String(value.Nonce);
        var ciphertext = Convert.FromBase64String(value.Ciphertext);
        var tag = Convert.FromBase64String(value.Tag);
        var plaintext = new byte[ciphertext.Length];
        using var aes = new AesGcm(ReadOrCreateKey(), tag.Length);
        aes.Decrypt(nonce, ciphertext, tag, plaintext);
        return Encoding.UTF8.GetString(plaintext);
    }

    private byte[] ReadOrCreateKey()
    {
        Directory.CreateDirectory(paths.SecretsRoot);
        var path = Path.Combine(paths.SecretsRoot, "ombi-user-sessions.key");
        if (File.Exists(path))
        {
            return Convert.FromBase64String(File.ReadAllText(path));
        }

        var key = RandomNumberGenerator.GetBytes(KeyBytes);
        File.WriteAllText(path, Convert.ToBase64String(key));
        return key;
    }
}

public sealed record OmbiUserSessionEnvelope(
    int Version,
    string OmbiUserName,
    string? DisplayName,
    string? OmbiUserId,
    string? ExpiresAt,
    IReadOnlyList<string> Roles,
    DateTimeOffset LastLoginAt,
    DateTimeOffset LastValidatedAt,
    EncryptedValue EncryptedAccessToken);

public sealed record EncryptedValue(string Nonce, string Ciphertext, string Tag);
