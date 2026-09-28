using System.Collections.Generic;

namespace Vantafyn.Plugin.Companion.Games;

public interface IGamesService
{
    IReadOnlyList<GameLibrary> GetGameLibraries();
    IReadOnlyList<GameSystem> GetSystems(string libraryId);
    IReadOnlyList<GameSummary> GetGames(string libraryId, string? system = null);
    GameDetail? GetGame(string libraryId, string gameId);
    string? ResolveFilePath(string libraryId, string token, bool allowBios);
    string? GetBoxartPath(string libraryId, string gameId);
    byte[]? ExtractRomFromArchive(string archivePath);
    ExtractedRomInfo? GetExtractedRomInfo(string archivePath);
    object GetDiagnostics();
}
