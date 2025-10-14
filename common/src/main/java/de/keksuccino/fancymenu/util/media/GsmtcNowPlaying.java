package de.keksuccino.fancymenu.util.media;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import de.keksuccino.fancymenu.util.terminal.CommandResult;
import de.keksuccino.fancymenu.util.terminal.PowerShellUtils;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convenience wrapper for a PowerShell script and exposes strongly-typed access to the current media session.
 * Only works on Windows 10/11.
 */
public final class GsmtcNowPlaying {

    private static final Gson GSON = new GsonBuilder().create();
    private static final Pattern JSON_NET_DATE = Pattern.compile("\\\\/Date\\((?<epoch>\\d+)\\)\\\\/");
    private static final String SCRIPT = """
            [CmdletBinding()]
            param(
              [switch]$AllSessions,
              [switch]$Json
            )
            
            $ErrorActionPreference = 'SilentlyContinue'
            
            # When invoked under PowerShell Core, hop into Windows PowerShell for WinRT support.
            if ($PSVersionTable.PSEdition -eq 'Core' -and $PSCommandPath) {
              if (-not $env:__GSMTC_NOWPLAYING_FALLBACK) {
                $env:__GSMTC_NOWPLAYING_FALLBACK = '1'
                $argsList = @('-NoLogo', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $PSCommandPath)
                if ($AllSessions) { $argsList += '-AllSessions' }
                if ($Json)        { $argsList += '-Json' }
                & "$env:SystemRoot\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" @argsList
                $exitCode = $LASTEXITCODE
                Remove-Item Env:__GSMTC_NOWPLAYING_FALLBACK -ErrorAction Ignore
                exit $exitCode
              } else {
                Remove-Item Env:__GSMTC_NOWPLAYING_FALLBACK -ErrorAction Ignore
              }
            }
            
            # Ensure WinRT interop helpers are available (needed on Windows PowerShell 5.1)
            try { Add-Type -AssemblyName System.Runtime.WindowsRuntime | Out-Null } catch {}
            
            function Get-WinRtType {
              param([Parameter(Mandatory)][string]$TypeName)
            
              foreach ($assemblyName in @(
                'Windows.Media.Control',
                'Windows.Media',
                'Windows.Storage.Streams',
                'Windows.Storage',
                'Windows.Foundation',
                'Windows'
              )) {
                $candidate = "$TypeName, $assemblyName, ContentType=WindowsRuntime"
                $resolved = [Type]::GetType($candidate, $false)
                if ($resolved) { return $resolved }
              }
              $null
            }
            
            function Invoke-WinRtAsyncOperation {
              param(
                [Parameter(Mandatory)][object]$Operation,
                [Parameter(Mandatory)][Type]$ResultType
              )
            
              if (-not $Operation -or -not $ResultType) { return $null }
            
              try {
                $opType = $Operation.GetType()
                $getAwaiter = $opType.GetMethod('GetAwaiter', [Type[]]@())
                if ($getAwaiter) {
                  $awaitable = $getAwaiter.Invoke($Operation, @())
                  if ($awaitable) {
                    $getResult = $awaitable.GetType().GetMethod('GetResult', [Type[]]@())
                    if ($getResult) {
                      return $getResult.Invoke($awaitable, @())
                    }
                  }
                }
              } catch {}
            
              try { Add-Type -AssemblyName System.Runtime.WindowsRuntime | Out-Null } catch {}
              try { $extensionsType = [System.WindowsRuntimeSystemExtensions] } catch { $extensionsType = $null }
              if ($extensionsType) {
                $method = $extensionsType.GetMethods() | Where-Object {
                  if ($_.Name -ne 'AsTask' -or -not $_.IsGenericMethodDefinition) { return $false }
                  $parameters = $_.GetParameters()
                  if ($parameters.Count -ne 1) { return $false }
                  $paramTypeName = $parameters[0].ParameterType.ToString()
                  return $paramTypeName.StartsWith('Windows.Foundation.IAsyncOperation`1')
                } | Select-Object -First 1
            
                if ($method) {
                  try {
                    $generic = $method.MakeGenericMethod($ResultType)
                    $task    = $generic.Invoke($null, @($Operation))
            
                    if ($task) {
                      $taskAwaiter = $task.GetType().GetMethod('GetAwaiter', [Type[]]@())
                      if ($taskAwaiter) {
                        $awaitable = $taskAwaiter.Invoke($task, @())
                        if ($awaitable) {
                          $getResult = $awaitable.GetType().GetMethod('GetResult', [Type[]]@())
                          if ($getResult) {
                            return $getResult.Invoke($awaitable, @())
                          }
                        }
                      }
            
                      if ($task -is [System.Threading.Tasks.Task]) {
                        $task.Wait()
                        $resultProp = $task.GetType().GetProperty('Result')
                        if ($resultProp) {
                          return $resultProp.GetValue($task, $null)
                        }
                      }
                    }
                  } catch {}
                }
              }
            
              $null
            }
            
            # Selects the first session whose playback status matches the provided preference order.
            function Get-SessionByStatusPriority {
              param(
                [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]$Manager,
                [string[]]$Statuses
              )
            
              if (-not $Manager) { return $null }
              $sessions = $Manager.GetSessions()
              if (-not $sessions) { return $null }
            
              foreach ($statusName in $Statuses) {
                $match = $sessions | Where-Object {
                  try {
                    $info = $_.GetPlaybackInfo()
                    if (-not $info) { return $false }
                    $status = $info.PlaybackStatus
                    if (-not $status) { return $false }
                    $status.ToString() -eq $statusName
                  } catch {
                    $false
                  }
                } | Select-Object -First 1
            
                if ($match) { return $match }
              }
            
              $sessions | Select-Object -First 1
            }
            
            # Cached WinRT helper types used later
            $randomAccessStreamType = Get-WinRtType 'Windows.Storage.Streams.IRandomAccessStreamWithContentType'
            try { $streamExtensionsType = [System.IO.WindowsRuntimeStreamExtensions] } catch { $streamExtensionsType = $null }
            
            # Get the manager
            $mgrType = Get-WinRtType 'Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager'
            $mgrOp   = if ($mgrType) { $mgrType::RequestAsync() } else { $null }
            $mgr     = if ($mgrOp)   { Invoke-WinRtAsyncOperation -Operation $mgrOp -ResultType $mgrType } else { $null }
            
            if (-not $mgr) {
              Write-Error "Couldn't get GSMTC session manager. Are you on Windows 10/11?"
              exit 1
            }
            
            $mediaPropsType = Get-WinRtType 'Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties'
            
            function Get-SessionObject {
              param([Windows.Media.Control.GlobalSystemMediaTransportControlsSession]$s)
              if (-not $s) { return $null }
              try {
                $propsOp  = $s.TryGetMediaPropertiesAsync()
                $props    = Invoke-WinRtAsyncOperation -Operation $propsOp -ResultType $mediaPropsType
                if (-not $props) { return $null }
                $timeline = $s.GetTimelineProperties()
                $playback = $s.GetPlaybackInfo()
                $thumbnailValue         = $null
                $thumbnailContentType   = ''
                $thumbnailBytes         = 0
            
                if ($props.Thumbnail -and $randomAccessStreamType -and $streamExtensionsType) {
                  try {
                    $thumbOp = $props.Thumbnail.OpenReadAsync()
                    $thumbStream = Invoke-WinRtAsyncOperation -Operation $thumbOp -ResultType $randomAccessStreamType
                    if ($thumbStream) {
                      try { $thumbnailContentType = $thumbStream.ContentType } catch { $thumbnailContentType = '' }
            
                      try {
                        $netStream = $streamExtensionsType::AsStreamForRead($thumbStream)
                        if ($netStream) {
                          $memoryStream = New-Object System.IO.MemoryStream
                          try {
                            $netStream.CopyTo($memoryStream)
                            $bytes = $memoryStream.ToArray()
                            if ($bytes -and $bytes.Length -gt 0) {
                              $thumbnailBytes       = $bytes.Length
                              $base64               = [Convert]::ToBase64String($bytes)
                              $thumbnailValue       = if ($thumbnailContentType) { "data:$thumbnailContentType;base64,$base64" } else { $base64 }
                            }
                          } finally {
                            $memoryStream.Dispose()
                            $netStream.Dispose()
                          }
                        }
                      } catch {}
            
                      try {
                        if ($thumbStream -is [System.IDisposable]) {
                          $thumbStream.Dispose()
                        } else {
                          $thumbStream.Close()
                        }
                      } catch {}
                    }
                  } catch {}
                }
            
                $status     = $playback.PlaybackStatus
                $statusName = if ($status) { $status.ToString() } else { '' }
                $isPlaying  = $statusName -eq 'Playing'
                $timelinePosition = if ($timeline.Position.Ticks -gt 0) { $timeline.Position.TotalSeconds } else { 0 }
                $timelineEnd      = if ($timeline.EndTime.Ticks   -gt 0) { $timeline.EndTime.TotalSeconds   } else { 0 }
                $positionSeconds  = $timelinePosition
            
                if ($isPlaying -and $timeline.LastUpdatedTime -and $timeline.LastUpdatedTime -ne [DateTimeOffset]::MinValue) {
                  try {
                    $elapsed = ([DateTimeOffset]::Now - $timeline.LastUpdatedTime).TotalSeconds
                    $rate = if ($playback -and $playback.PlaybackRate) { $playback.PlaybackRate } else { 1.0 }
                    if ($elapsed -gt 0 -and $rate) {
                      $positionSeconds = $timelinePosition + ($elapsed * $rate)
                    }
                  } catch {}
                }
            
                if ($timelineEnd -gt 0) {
                  if ($positionSeconds -gt $timelineEnd) { $positionSeconds = $timelineEnd }
                  if ($positionSeconds -lt 0) { $positionSeconds = 0 }
                }
            
                $pos = [math]::Round($positionSeconds)
                $dur = [math]::Round($timelineEnd)
            
                [pscustomobject]@{
                  appId       = $s.SourceAppUserModelId
                  title       = $props.Title
                  artist      = $props.Artist
                  album       = $props.AlbumTitle
                  albumArtist = $props.AlbumArtist
                  genres      = ($props.Genres -join ', ')
                  trackNumber = $props.TrackNumber
                  playback    = $statusName
                  playing     = $isPlaying
                  position    = $pos
                  duration    = $dur
                  thumbnail   = $thumbnailValue
                  thumbnailContentType = $thumbnailContentType
                  thumbnailBytes       = $thumbnailBytes
                  lastUpdated = if ($timeline.LastUpdatedTime) { $timeline.LastUpdatedTime } else { $null }
                  playbackRate = if ($playback -and $playback.PlaybackRate) { $playback.PlaybackRate } else { $null }
                }
              } catch {
                $null
              }
            }
            
            if ($AllSessions) {
              $objs = @()
              foreach ($s in $mgr.GetSessions()) {
                $o = Get-SessionObject $s
                if ($o) { $objs += $o }
              }
              if ($Json) {
                $objs | ConvertTo-Json -Compress
              } else {
                $objs | Format-Table -Property appId,title,artist,playback,playing,position,duration -AutoSize
              }
              exit 0
            }
            
            # Default path: current session; if not playing, pick the first playing session
            $cur = $mgr.GetCurrentSession()
            $curStatusName = ''
            if ($cur) {
              try {
                $currentStatus = $cur.GetPlaybackInfo().PlaybackStatus
                $curStatusName = if ($currentStatus) { $currentStatus.ToString() } else { '' }
              } catch {
                $curStatusName = ''
              }
            }
            
            $preferredStatuses = @('Playing', 'Paused', 'Changing', 'Stopped')
            
            if (-not $cur -or $curStatusName -ne 'Playing') {
              $candidate = Get-SessionByStatusPriority -Manager $mgr -Statuses $preferredStatuses
              if ($candidate) {
                $cur = $candidate
              }
            }
            
            $obj = Get-SessionObject $cur
            if ($obj) {
              if ($Json) { $obj | ConvertTo-Json -Compress } else { $obj | Format-List }
              exit 0
            } else {
            if ($Json) { '{}' } else { Write-Host "No active media session found." }
            exit 2
          }
    """;
    private static final String PERSISTENT_MODULE_SCRIPT = buildPersistentModuleScript();
    private static final PersistentPowerShell PERSISTENT = new PersistentPowerShell();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(PERSISTENT::close, "GsmtcNowPlaying-PS-Shutdown"));
    }

    private GsmtcNowPlaying() {
        // Utility class
    }

    /**
     * Returns the current session's media info, if any.
     */
    public static Optional<MediaInfo> getCurrentSession() throws IOException, InterruptedException {
        CommandResult result = runScript(false);
        if (result.exitCode() == 0) {
            return Optional.ofNullable(parseMediaInfo(result.output()));
        }
        if (result.exitCode() == 2) {
            return Optional.empty();
        }
        throw new IOException(buildErrorMessage(result));
    }

    private static MediaInfo parseMediaInfo(String json) throws JsonSyntaxException {
        String trimmed = json == null ? "" : json.trim();
        if (trimmed.isEmpty() || "{}".equals(trimmed)) {
            return null;
        }
        return GSON.fromJson(trimmed, MediaInfo.class);
    }

    private static CommandResult runScript(boolean allSessions) throws InterruptedException {
        try {
            return PERSISTENT.invoke(allSessions);
        } catch (IOException | InterruptedException e) {
            PERSISTENT.invalidate();
            if (e instanceof InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw interrupted;
            }
            return new CommandResult(2, "");
        }
    }

    private static String buildPersistentModuleScript() {
        String normalized = SCRIPT.replace("\r\n", "\n");
        String[] lines = normalized.split("\n", -1);
        StringBuilder body = new StringBuilder();
        boolean paramStarted = false;
        boolean paramClosed = false;
        boolean resetInserted = false;

        for (String line : lines) {
            String trimmed = line.trim();
            int leadingCount = line.length() - trimmed.length();
            if (leadingCount < 0) {
                leadingCount = 0;
            }
            String leading = line.substring(0, leadingCount);
            boolean isExit = false;
            if (trimmed.startsWith("exit")) {
                if (trimmed.length() == 4 || Character.isWhitespace(trimmed.charAt(4))) {
                    isExit = true;
                }
            }

            if (isExit) {
                String exitArg = trimmed.substring(4).trim();
                if (exitArg.isEmpty()) {
                    exitArg = "0";
                }
                String indent = "    " + leading;
                body.append(indent).append("$script:__GsmtcExitCode = ").append(exitArg).append('\n');
                body.append(indent).append("return\n");
                continue;
            }

            body.append("    ").append(line).append('\n');

            if (!paramStarted && trimmed.startsWith("param(")) {
                paramStarted = true;
            } else if (paramStarted && !paramClosed) {
                if (trimmed.equals(")")) {
                    paramClosed = true;
                    if (!resetInserted) {
                        body.append("    Set-Variable -Scope Script -Name __GsmtcExitCode -Value 0\n");
                        resetInserted = true;
                    }
                }
            }
        }

        if (!resetInserted) {
            body.insert(0, "    Set-Variable -Scope Script -Name __GsmtcExitCode -Value 0\n");
        }

        StringBuilder module = new StringBuilder();
        module.append("$script:__GsmtcExitCode = 0\n");
        module.append("function Invoke-GsmtcNowPlayingCore {\n");
        module.append(body);
        module.append("}\n");
        module.append("function Invoke-GsmtcNowPlayingRunner {\n");
        module.append("    param([switch]$AllSessions, [switch]$Json)\n");
        module.append("    $result = (& { Invoke-GsmtcNowPlayingCore -AllSessions:$AllSessions -Json:$Json } *>&1) | Out-String\n");
        module.append("    [pscustomobject]@{\n");
        module.append("        ExitCode = $script:__GsmtcExitCode\n");
        module.append("        Output = $result\n");
        module.append("    }\n");
        module.append("}\n");
        return module.toString();
    }

    private static String buildErrorMessage(CommandResult result) {
        return "PowerShell exited with " + result.exitCode() + (result.output().isBlank() ? "" : " Output: " + result.output().trim());
    }

    /**
     * DTO representing the media info JSON returned by the PowerShell script.
     */
    @SuppressWarnings("unused")
    public static final class MediaInfo {

        private String appId;
        private String title;
        private String artist;
        private String album;
        private String albumArtist;
        private String genres;
        private Integer trackNumber;
        private String playback;
        private Boolean playing;
        private Integer position;
        private Integer duration;
        private String thumbnail;
        private String thumbnailContentType;
        private Integer thumbnailBytes;
        private String lastUpdated;
        private Double playbackRate;

        public String getAppId() {
            return appId;
        }

        public String getTitle() {
            return title;
        }

        public String getArtist() {
            return artist;
        }

        public String getAlbum() {
            return album;
        }

        public String getAlbumArtist() {
            return albumArtist;
        }

        public String getGenres() {
            return genres;
        }

        public Integer getTrackNumber() {
            return trackNumber;
        }

        public String getPlayback() {
            return playback;
        }

        public boolean isPlaying() {
            return Boolean.TRUE.equals(playing);
        }

        public Integer getPositionSeconds() {
            return position;
        }

        public Integer getDurationSeconds() {
            return duration;
        }

        public String getThumbnail() {
            return thumbnail;
        }

        public String getThumbnailContentType() {
            return thumbnailContentType;
        }

        public Integer getThumbnailBytes() {
            return thumbnailBytes;
        }

        public String getLastUpdatedRaw() {
            return lastUpdated;
        }

        public Double getPlaybackRate() {
            return playbackRate;
        }

        public Instant getLastUpdatedInstant() {
            if (lastUpdated == null) {
                return null;
            }
            Matcher matcher = JSON_NET_DATE.matcher(lastUpdated);
            if (matcher.matches()) {
                long epochMillis = Long.parseLong(matcher.group("epoch"));
                return Instant.ofEpochMilli(epochMillis);
            }
            return null;
        }

        @Override
        public String toString() {
            return "MediaInfo{" +
                    "appId='" + appId + '\'' +
                    ", title='" + title + '\'' +
                    ", artist='" + artist + '\'' +
                    ", playback='" + playback + '\'' +
                    ", playing=" + playing +
                    ", position=" + position +
                    ", duration=" + duration +
                    '}';
        }

        @Override
        public int hashCode() {
            return Objects.hash(appId, title, artist, album, playback, playing, position, duration);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MediaInfo other)) {
                return false;
            }
            return Objects.equals(appId, other.appId)
                    && Objects.equals(title, other.title)
                    && Objects.equals(artist, other.artist)
                    && Objects.equals(album, other.album)
                    && Objects.equals(albumArtist, other.albumArtist)
                    && Objects.equals(playback, other.playback)
                    && Objects.equals(playing, other.playing)
                    && Objects.equals(position, other.position)
                    && Objects.equals(duration, other.duration);
        }

    }

    private static class PersistentPowerShell {

        public static final String READY_MARKER = "__READY__";
        public static final String END_MARKER = "__END__";

        public Process process;
        public BufferedWriter writer;
        public BufferedReader reader;

        public synchronized CommandResult invoke(boolean allSessions) throws IOException, InterruptedException {
            ensureStarted();
            String command = buildInvocation(allSessions);
            writer.write(command);
            writer.flush();
            PersistentPowerShell.PersistentEnvelope envelope = readEnvelope();
            String output = envelope.output == null ? "" : envelope.output;
            return new CommandResult(envelope.exitCode, output);
        }

        public synchronized void invalidate() {
            close();
        }

        public synchronized void close() {
            if (writer != null) {
                try {
                    writer.write("exit\n");
                    writer.flush();
                } catch (IOException ignore) {
                    // ignore because we are shutting down
                }
            }
            if (process != null) {
                process.destroy();
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            closeQuietly(writer);
            closeQuietly(reader);
            process = null;
            writer = null;
            reader = null;
        }

        public void ensureStarted() throws IOException {
            if (process != null && process.isAlive()) {
                return;
            }
            startProcess();
        }

        public void startProcess() throws IOException {
            close();
            List<String> command = new ArrayList<>();
            command.add(PowerShellUtils.locatePowerShell());
            command.add("-NoLogo");
            command.add("-NoProfile");
            command.add("-ExecutionPolicy");
            command.add("Bypass");
            command.add("-Command");
            command.add("-");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            process = pb.start();
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            initialize();
        }

        public void initialize() throws IOException {

            String scriptBase64 = Base64.getEncoder().encodeToString(PERSISTENT_MODULE_SCRIPT.getBytes(StandardCharsets.UTF_8));
            writer.write("[Console]::InputEncoding = [System.Text.Encoding]::UTF8\n");
            writer.write("[Console]::OutputEncoding = [System.Text.Encoding]::UTF8\n");
            writer.write("$__gsmtc_script = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String('" + scriptBase64 + "'))\n");
            writer.write("Invoke-Expression $__gsmtc_script\n");
            writer.write("Write-Output '" + READY_MARKER + "'\n");
            writer.flush();

            String line;
            while ((line = reader.readLine()) != null) {
                if (READY_MARKER.equals(line)) {
                    return;
                }
                if (line.isBlank()) {
                    continue;
                }
            }
            close();
            throw new IOException("Failed to initialise persistent PowerShell host.");
        }

        public String buildInvocation(boolean allSessions) {
            String allSessionsFlag = allSessions ? "$true" : "$false";
            return "$__gsmtc_result = Invoke-GsmtcNowPlayingRunner -AllSessions:" + allSessionsFlag + " -Json:$true\n"
                    + "$__gsmtc_json = $__gsmtc_result | ConvertTo-Json -Compress\n"
                    + "Write-Output $__gsmtc_json\n"
                    + "Write-Output '" + END_MARKER + "'\n";
        }

        public PersistentPowerShell.PersistentEnvelope readEnvelope() throws IOException {
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (END_MARKER.equals(line)) {
                    break;
                }
                if (!buffer.isEmpty()) {
                    buffer.append('\n');
                }
                buffer.append(line);
            }
            if (line == null) {
                throw new IOException("Persistent PowerShell host terminated unexpectedly.");
            }
            String json = buffer.toString();
            if (json.isBlank()) {
                throw new IOException("Persistent PowerShell host returned empty payload.");
            }
            try {
                PersistentPowerShell.PersistentEnvelope envelope = GSON.fromJson(json, PersistentPowerShell.PersistentEnvelope.class);
                if (envelope == null) {
                    throw new IOException("Unable to parse persistent PowerShell response.");
                }
                return envelope;
            } catch (JsonSyntaxException ex) {
                throw new IOException("Unable to parse persistent PowerShell response.", ex);
            }
        }

        public static void closeQuietly(AutoCloseable closeable) {
            if (closeable == null) {
                return;
            }
            try {
                closeable.close();
            } catch (Exception ignore) {
                // ignore
            }
        }

        public static final class PersistentEnvelope {
            @SerializedName("ExitCode")
            int exitCode;

            @SerializedName("Output")
            String output;
        }

    }

}
