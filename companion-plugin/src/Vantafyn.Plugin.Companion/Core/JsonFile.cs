using System.Text;
using System.Text.Json;

namespace Vantafyn.Plugin.Companion.Core;

public static class JsonFile
{
    private static readonly JsonSerializerOptions Options = new(JsonSerializerDefaults.Web)
    {
        WriteIndented = true
    };

    public static async Task<T?> ReadAsync<T>(string path, CancellationToken cancellationToken)
    {
        if (!File.Exists(path)) return default;
        await using var stream = File.OpenRead(path);
        return await JsonSerializer.DeserializeAsync<T>(stream, Options, cancellationToken).ConfigureAwait(false);
    }

    public static async Task WriteAtomicAsync<T>(string path, T value, CancellationToken cancellationToken)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(path)!);
        var temp = path + ".tmp";
        await using (var stream = new FileStream(temp, FileMode.Create, FileAccess.Write, FileShare.None))
        {
            await JsonSerializer.SerializeAsync(stream, value, Options, cancellationToken).ConfigureAwait(false);
            await stream.FlushAsync(cancellationToken).ConfigureAwait(false);
        }

        if (File.Exists(path))
        {
            File.Replace(temp, path, null);
        }
        else
        {
            File.Move(temp, path);
        }
    }

    public static void ValidateJsonPayload(string json, int maxBytes)
    {
        if (Encoding.UTF8.GetByteCount(json) > maxBytes)
        {
            throw new PayloadTooLargeException($"Payload exceeds {maxBytes} bytes.");
        }

        using var _ = JsonDocument.Parse(json);
    }
}

public sealed class PayloadTooLargeException(string message) : Exception(message);
public sealed class RevisionConflictException(int currentRevision) : Exception("The stored settings revision is newer.")
{
    public int CurrentRevision { get; } = currentRevision;
}
