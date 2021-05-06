/* Spotify Web API, Kotlin Wrapper; MIT License, 2017-2021; Original author: Adam Ratzman */
package com.adamratzman.spotify.priv

import com.adamratzman.spotify.SpotifyClientApi
import com.adamratzman.spotify.buildSpotifyApi
import com.adamratzman.spotify.endpoints.client.ClientPlayerApi
import com.adamratzman.spotify.models.CurrentlyPlayingType
import com.adamratzman.spotify.models.PlayableUri
import com.adamratzman.spotify.models.SpotifyContextType
import com.adamratzman.spotify.models.SpotifyTrackUri
import com.adamratzman.spotify.models.toAlbumUri
import com.adamratzman.spotify.models.toArtistUri
import com.adamratzman.spotify.models.toPlaylistUri
import com.adamratzman.spotify.models.toShowUri
import com.adamratzman.spotify.runBlockingTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlinx.coroutines.delay

@ExperimentalTime
class ClientPlayerApiTest {
    var api: SpotifyClientApi? = null

    init {
        runBlockingTest {
            (buildSpotifyApi() as? SpotifyClientApi)?.let { api = it }
        }
    }

    fun testPrereq() = api != null

    @Test
    fun testGetDevices() {
        runBlockingTest {
            if (!testPrereq()) return@runBlockingTest else api!!
            assertTrue(api!!.player.getDevices().isNotEmpty())
        }
    }

    @Test
    fun testGetCurrentContext() {
        runBlockingTest {
            if (!testPrereq()) return@runBlockingTest else api!!
            val device = api!!.player.getDevices().first()
            api!!.player.startPlayback(
                playableUrisToPlay = listOf(SpotifyTrackUri("spotify:track:6WcinC5nKan2DMFUfjVerX")),
                deviceId = device.id
            )
            delay(1000)
            val getCurrentContext = suspend { api!!.player.getCurrentContext() }
            var context = getCurrentContext()
            assertTrue(context != null && context.isPlaying && context.track?.id == "6WcinC5nKan2DMFUfjVerX")
            api!!.player.pause()
            context = getCurrentContext()!!

            assertTrue(!context.isPlaying)
            assertNotNull(context.track?.id)

            val playlist = api!!.playlists.getPlaylist("37i9dQZF1DXcBWIGoYBM5M")!!
            api!!.player.startPlayback(
                contextUri = playlist.uri
            )
            delay(1000)
            context = getCurrentContext()
            assertTrue(context != null && context.isPlaying && context.track?.id == playlist.tracks.items.first().track!!.id)
            api!!.player.pause()
        }
    }

    @Test
    fun testGetRecentlyPlayed() {
        runBlockingTest {
            if (!testPrereq()) return@runBlockingTest else api!!
            api!!.player.getRecentlyPlayed()
        }
    }

    @Test
    fun testGetCurrentlyPlaying() {
        runBlockingTest {
            if (!testPrereq()) return@runBlockingTest else api!!
            val device = api!!.player.getDevices().first()

            val trackId = "7lPN2DXiMsVn7XUKtOW1CS"
            api!!.player.startPlayback(
                playableUrisToPlay = listOf(PlayableUri("spotify:track:$trackId")),
                deviceId = device.id
            )
            delay(1000)
            val currentlyPlayingObjectTrack = api!!.player.getCurrentlyPlaying()
            assertNotNull(currentlyPlayingObjectTrack)
            assertTrue(currentlyPlayingObjectTrack.isPlaying && currentlyPlayingObjectTrack.context == null)

            val playlistId = "3DhwYIoAZ8mXlxiBkCuOx7"
            api!!.player.startPlayback(contextUri = playlistId.toPlaylistUri())
            delay(1000)
            val currentlyPlayingObjectPlaylist = api!!.player.getCurrentlyPlaying()
            assertNotNull(currentlyPlayingObjectPlaylist)
            assertTrue(currentlyPlayingObjectPlaylist.isPlaying)
            assertEquals(playlistId, currentlyPlayingObjectPlaylist.context?.uri?.id)
            assertEquals(SpotifyContextType.Playlist, currentlyPlayingObjectPlaylist.context?.type)

            api!!.player.pause()
        }
    }

    @Test
    fun testSeek() {
        runBlockingTest {
            if (!testPrereq()) return@runBlockingTest else api!!
            val device = api!!.player.getDevices().first()

            val trackId = "7lPN2DXiMsVn7XUKtOW1CS"
            val track = api!!.tracks.getTrack(trackId)!!
            api!!.player.startPlayback(
                playableUrisToPlay = listOf(PlayableUri("spotify:track:$trackId")),
                deviceId = device.id
            )
            api!!.player.pause()

            val skipTo = track.length / 2
            val delay = measureTime {
                api!!.player.seek(skipTo.toLong())
                api!!.player.resume()
            }.inMilliseconds

            val waitTime = 3000
            delay(waitTime.toLong())
            assertTrue(api!!.player.getCurrentlyPlaying()!!.progressMs!! >= waitTime - delay)
            api!!.player.skipForward()
        }
    }

    @Test
    fun testSetPlaybackOptions() {
        runBlockingTest {
            if (!testPrereq()) return@runBlockingTest else api!!
            val device = api!!.player.getDevices().first()
            val volume = 50
            api!!.player.setRepeatMode(ClientPlayerApi.PlayerRepeatState.OFF, device.id)
            api!!.player.setVolume(volume, device.id)
            api!!.player.toggleShuffle(shuffle = true)
            val context = api!!.player.getCurrentContext()!!
            assertEquals(ClientPlayerApi.PlayerRepeatState.OFF, context.repeatState)
            assertEquals(volume, context.device.volumePercent)
            assertEquals(true, context.shuffleState)
            api!!.player.toggleShuffle(shuffle = false)
            assertEquals(false, api!!.player.getCurrentContext()!!.shuffleState)
        }
    }

    @Test
    fun testStartPlayback() {
        runBlockingTest {
            if (!testPrereq()) return@runBlockingTest else api!!
            val device = api!!.player.getDevices().first()

            val playlistUri = "spotify:playlist:37i9dQZF1DXcBWIGoYBM5M".toPlaylistUri()
            val artistUri = "spotify:artist:0MlOPi3zIDMVrfA9R04Fe3".toArtistUri()
            val showUri = "spotify:show:6z4NLXyHPga1UmSJsPK7G1".toShowUri()
            val albumUri = "spotify:album:7qmzJKB20IS9non9kBkPgF".toAlbumUri()
            // play from a context
            api!!.player.startPlayback(contextUri = playlistUri, deviceId = device.id)
            api!!.player.skipForward()
            delay(1000)
            assertEquals(playlistUri, api!!.player.getCurrentContext()?.context?.uri)

            api!!.player.startPlayback(contextUri = artistUri, deviceId = device.id)
            delay(1000)
            assertEquals(artistUri, api!!.player.getCurrentContext()?.context?.uri)

            api!!.player.startPlayback(contextUri = showUri, deviceId = device.id)
            delay(2000)
            // can't check more specifics because context/track are both null for episodes (for some reason?)
            assertEquals(
                CurrentlyPlayingType.EPISODE,
                api!!.player.getCurrentlyPlaying()?.currentlyPlayingType
            )

            api!!.player.startPlayback(contextUri = albumUri, deviceId = device.id)
            delay(1000)
            assertEquals(albumUri, api!!.player.getCurrentContext()?.context?.uri)

            // play tracks normally
            val trackUris = api!!.playlists.getPlaylist(playlistUri.id)!!.tracks.take(5).mapNotNull { it.track?.asTrack }.map { it.uri }
            api!!.player.startPlayback(
                playableUrisToPlay = trackUris
            )
            delay(1000)
            assertEquals(trackUris.first().id, api!!.player.getCurrentlyPlaying()?.track?.id)
            api!!.player.skipForward()
            delay(1000)
            assertEquals(trackUris[1].id, api!!.player.getCurrentlyPlaying()?.track?.id)

            // play tracks with offset index
            val offsetIndex = 2
            api!!.player.startPlayback(playableUrisToPlay = trackUris, offsetIndex = offsetIndex)
            delay(1000)
            assertEquals(trackUris[2].id, api!!.player.getCurrentlyPlaying()?.track?.id)
            api!!.player.skipForward()
            delay(1000)
            assertEquals(trackUris[offsetIndex + 1].id, api!!.player.getCurrentlyPlaying()?.track?.id)

            // play tracks with offset track
            val offsetTrackUri = trackUris[offsetIndex]
            api!!.player.startPlayback(playableUrisToPlay = trackUris, offsetPlayableUri = offsetTrackUri)
            delay(1000)
            assertEquals(offsetTrackUri.id, api!!.player.getCurrentlyPlaying()?.track?.id)
            api!!.player.skipForward()
            delay(1000)
            assertEquals(trackUris[offsetIndex + 1].id, api!!.player.getCurrentlyPlaying()?.track?.id)

            // play playlist with offset track
            api!!.player.startPlayback(contextUri = playlistUri, offsetIndex = offsetIndex)
            delay(1000)
            assertEquals(trackUris[offsetIndex].id, api!!.player.getCurrentlyPlaying()?.track?.id)
            api!!.player.skipForward()
            delay(1000)
            assertEquals(trackUris[offsetIndex + 1].id, api!!.player.getCurrentlyPlaying()?.track?.id)
        }
    }

    @Test
    fun testSkipForwardBackward() {
        runBlockingTest {
            if (!testPrereq()) return@runBlockingTest else api!!
            val device = api!!.player.getDevices().first()

            val playlist = api!!.playlists.getPlaylist("37i9dQZF1DXcBWIGoYBM5M")!!
            api!!.player.startPlayback(
                contextUri = playlist.uri,
                deviceId = device.id
            )
            delay(1000)

            api!!.player.skipForward()
            delay(500)
            assertEquals(playlist.tracks[1].track!!.id, api!!.player.getCurrentlyPlaying()!!.track?.id)

            api!!.player.skipBehind()
            delay(500)
            assertEquals(playlist.tracks[0].track!!.id, api!!.player.getCurrentlyPlaying()!!.track?.id)

            api!!.player.pause()
        }
    }

    @Test
    fun testTransferPlayback() {
        runBlockingTest {
            if (!testPrereq()) return@runBlockingTest else api!!
            if (api!!.player.getDevices().size < 2) {
                println("Active devices < 2 (${api!!.player.getDevices()}), so skipping transfer playback test")
                return@runBlockingTest
            }
            val devices = api!!.player.getDevices()
            val fromDevice = devices.first()
            val toDevice = devices[1]

            api!!.player.startPlayback(
                playableUrisToPlay = listOf(PlayableUri("spotify:track:7lPN2DXiMsVn7XUKtOW1CS")),
                deviceId = fromDevice.id
            )
            delay(1000)

            api!!.player.transferPlayback(
                deviceId = toDevice.id!!
            )
            delay(3000)

            assertEquals(toDevice.id, api!!.player.getCurrentContext()!!.device.id)
        }
    }
}
