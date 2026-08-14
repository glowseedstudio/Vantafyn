using MediaBrowser.Controller.Net;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System.Text;
using Vantafyn.Plugin.Companion.Core;

namespace Vantafyn.Plugin.Companion.WatchParties;

[ApiController]
[Authorize]
[Route("Vantafyn")]
public sealed class EventsController(
    IAuthorizationContext authorizationContext,
    IRealtimeTransport realtime) : ControllerBase
{
    [HttpGet("Events")]
    public async Task Events(CancellationToken cancellationToken)
    {
        var userId = this.CurrentUserId(authorizationContext);
        Response.Headers.ContentType = "text/event-stream";
        Response.Headers.CacheControl = "no-cache";

        await foreach (var message in realtime.SubscribeAsync(userId, cancellationToken).ConfigureAwait(false))
        {
            var bytes = Encoding.UTF8.GetBytes($"event: vantafyn\ndata: {message}\n\n");
            await Response.Body.WriteAsync(bytes, cancellationToken).ConfigureAwait(false);
            await Response.Body.FlushAsync(cancellationToken).ConfigureAwait(false);
        }
    }
}
