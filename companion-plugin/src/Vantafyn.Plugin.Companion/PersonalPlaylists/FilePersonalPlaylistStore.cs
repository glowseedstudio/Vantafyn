using System.Collections.Concurrent;
using System.Security.Cryptography;
using Vantafyn.Plugin.Companion.Core;

namespace Vantafyn.Plugin.Companion.PersonalPlaylists;

public interface IPersonalPlaylistStore
{
    Task<IReadOnlyList<PersonalPlaylist>> ListAsync(Guid userId, CancellationToken cancellationToken);
    Task<PersonalPlaylist> CreateAsync(Guid userId, CreatePersonalPlaylistRequest request, CancellationToken cancellationToken);
    Task<PersonalPlaylist?> AddItemAsync(Guid userId, string playlistId, AddPersonalPlaylistItemRequest request, CancellationToken cancellationToken);
}

public sealed class FilePersonalPlaylistStore(ICompanionPaths paths, IClock clock) : IPersonalPlaylistStore
{
    private static readonly ConcurrentDictionary<Guid, SemaphoreSlim> Locks = new();

    public async Task<IReadOnlyList<PersonalPlaylist>> ListAsync(Guid userId, CancellationToken cancellationToken)
    {
        await LockFor(userId).WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            return await ReadAsync(userId, cancellationToken).ConfigureAwait(false);
        }
        finally
        {
            LockFor(userId).Release();
        }
    }

    public async Task<PersonalPlaylist> CreateAsync(Guid userId, CreatePersonalPlaylistRequest request, CancellationToken cancellationToken)
    {
        await LockFor(userId).WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var playlists = (await ReadAsync(userId, cancellationToken).ConfigureAwait(false)).ToList();
            var now = clock.UtcNow;
            var playlist = new PersonalPlaylist(NewOpaqueId("pl"), userId, request.Name.Trim(), request.Description?.Trim(), now, now, Array.Empty<PersonalPlaylistItem>());
            playlists.Add(playlist);
            await WriteAsync(userId, playlists, cancellationToken).ConfigureAwait(false);
            return playlist;
        }
        finally
        {
            LockFor(userId).Release();
        }
    }

    public async Task<PersonalPlaylist?> AddItemAsync(Guid userId, string playlistId, AddPersonalPlaylistItemRequest request, CancellationToken cancellationToken)
    {
        await LockFor(userId).WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var playlists = (await ReadAsync(userId, cancellationToken).ConfigureAwait(false)).ToList();
            var index = playlists.FindIndex(x => x.Id == playlistId);
            if (index < 0) return null;
            var playlist = playlists[index];
            var items = playlist.Items.ToList();
            if (items.All(x => x.JellyfinItemId != request.JellyfinItemId))
            {
                items.Add(new PersonalPlaylistItem(request.JellyfinItemId, items.Count, clock.UtcNow));
            }
            playlists[index] = playlist with { UpdatedAtUtc = clock.UtcNow, Items = items };
            await WriteAsync(userId, playlists, cancellationToken).ConfigureAwait(false);
            return playlists[index];
        }
        finally
        {
            LockFor(userId).Release();
        }
    }

    private async Task<IReadOnlyList<PersonalPlaylist>> ReadAsync(Guid userId, CancellationToken cancellationToken) =>
        await JsonFile.ReadAsync<List<PersonalPlaylist>>(PathFor(userId), cancellationToken).ConfigureAwait(false) ?? new List<PersonalPlaylist>();

    private Task WriteAsync(Guid userId, IReadOnlyList<PersonalPlaylist> playlists, CancellationToken cancellationToken) =>
        JsonFile.WriteAtomicAsync(PathFor(userId), playlists, cancellationToken);

    private string PathFor(Guid userId) => Path.Combine(paths.PersonalPlaylistsRoot, userId.ToString("N") + ".json");
    private static SemaphoreSlim LockFor(Guid userId) => Locks.GetOrAdd(userId, _ => new SemaphoreSlim(1, 1));

    private static string NewOpaqueId(string prefix)
    {
        Span<byte> bytes = stackalloc byte[16];
        RandomNumberGenerator.Fill(bytes);
        return prefix + "_" + Convert.ToBase64String(bytes).Replace('+', '-').Replace('/', '_').TrimEnd('=');
    }
}
