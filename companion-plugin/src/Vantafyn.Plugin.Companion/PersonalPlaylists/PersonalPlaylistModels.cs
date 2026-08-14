namespace Vantafyn.Plugin.Companion.PersonalPlaylists;

public sealed record PersonalPlaylistItem(string JellyfinItemId, int SortOrder, DateTimeOffset AddedAtUtc);

public sealed record PersonalPlaylist(
    string Id,
    Guid OwnerUserId,
    string Name,
    string? Description,
    DateTimeOffset CreatedAtUtc,
    DateTimeOffset UpdatedAtUtc,
    IReadOnlyList<PersonalPlaylistItem> Items);

public sealed record CreatePersonalPlaylistRequest(string Name, string? Description);
public sealed record AddPersonalPlaylistItemRequest(string JellyfinItemId);
