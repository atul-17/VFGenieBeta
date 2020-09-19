package com.libre.alexa.app.dlna.dmc.utility;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.libre.alexa.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.libre.alexa.nowplaying.NowPlayingFragment;
import com.libre.alexa.util.LibreLogger;

import org.fourthline.cling.model.ModelUtil;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Action;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.item.Item;

import java.util.List;
import java.util.Random;


public class PlaybackHelper implements DMRProcessor.DMRProcessorListener {
    public static Context MAIN_CONTEXT;
    private final static String TAG = PlaybackHelper.class.getSimpleName();
    private DMRControlHelper dmrHelper;
    private DMSBrowseHelper dmsHelper;
    private String singer = "";
    private String songName = "";
    private Bitmap songImage = null;
    private int durationSeconds = 0; //seconds
    private int relTime = 0; //seconds
    private boolean isPlaying = false;
    private String URL;
    private String META;
    private int volume;


    /*this is made for shuffle feature while local content*/
    public void setIsShuffleOn(boolean isShuffleOn) {
        this.isShuffleOn = isShuffleOn;
    }

    public boolean isShuffleOn() {
        return isShuffleOn;
    }

    private boolean isShuffleOn;

    public int getRepeatState() {
        return repeatState;
    }

    public void setRepeatState(int repeatState) {
        this.repeatState = repeatState;
    }

    private int repeatState;


    public String getSinger() {
        return singer;
    }

    public String getSongName() {
        return songName;
    }

    public Bitmap getSongImage() {
        return songImage;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getRelTime() {
        return relTime;
    }

    public DMRControlHelper getDmrHelper() {
        return dmrHelper;
    }


    public DMSBrowseHelper getDmsHelper() {
        return dmsHelper;
    }

    public void setDmsHelper(DMSBrowseHelper dmsHelper) {
        this.dmsHelper = dmsHelper;
    }

    public void addDmrProcessorListener(){
        if(mStopPlayBackCalled) {
            mStopPlayBackCalled=false;
            dmrHelper.getDmrProcessor().addListener(this);
        }
    }
    public PlaybackHelper(DMRControlHelper dmr) {
        dmrHelper = dmr;
        dmrHelper.getDmrProcessor().addListener(this);
    }

    private boolean mStopPlayBackCalled = false;

    public boolean getPlaybackStopped(){
        return mStopPlayBackCalled;
    }
    @Override
    public String toString() {
        return dmrHelper.getDeviceUdn();
    }

    public boolean StopPlayback() {

        dmrHelper.getDmrProcessor().stop();
        dmrHelper.getDmrProcessor().removeListener(this);
        mStopPlayBackCalled=true;
        return true;
    }


    public void playSong() {
        LibreLogger.d(this,"PlaySong");
        if (dmsHelper == null) return;
        DIDLObject didlObj = dmsHelper.getDIDLObject();
        if (didlObj == null || !(didlObj instanceof Item)) return;

        String url = null;
        if (didlObj.getResources() != null && didlObj.getResources().get(0) != null) {
            url = didlObj.getResources().get(0).getValue();
        }

        if (url == null) return;
        String title = didlObj.getTitle();
        String creator = didlObj.getCreator();
//		String cls = didlObj.getClass();
        String album = didlObj.getFirstPropertyValue(DIDLObject.Property.UPNP.ALBUM.class);
        java.net.URI uri = didlObj.getFirstPropertyValue(DIDLObject.Property.UPNP.ALBUM_ART_URI.class);
        String artist = didlObj.getCreator();
        String prtocolinfo = didlObj.getFirstResource().getProtocolInfo().getContentFormat();
        String size;
        /*this is added to avoid NPE reported by Crashlytics //// AFAIK waste code*/
        if (didlObj.getFirstResource().getSize() != null) {
            size = didlObj.getFirstResource().getSize().toString();
        } else {
            size = "1024";
        }
        String duration = didlObj.getFirstResource().getDuration();


        String urlMeta = null;
        if (duration != null) {
            duration = duration.indexOf(".") == -1 ? duration : duration.substring(0, duration.indexOf("."));
            urlMeta = "<DIDL-Lite xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\" xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\">"
                    + "<item id=\"audio-item-293\" parentID=\"2\" restricted=\"0\">"
                    + "<dc:title>"
                    + "<![CDATA["+title+"]]>"
                    + "</dc:title>"
                    + "<dc:creator>"
                    + "<![CDATA["+creator+"]]>"
                    + "</dc:creator>"
                    + "<upnp:class>object.item.audioItem.musicTrack</upnp:class>"
                    + "<upnp:album>"
                    + "<![CDATA["+album+"]]>"
                    + "</upnp:album>"
                    + "<upnp:albumArtURI>"
                    + uri
                    + "</upnp:albumArtURI>"
                    + "<upnp:artist role=\"Performer\">"
                    + "<![CDATA["+artist+"]]>"
                    + "</upnp:artist>"
                    + "<res protocolInfo=\"http-get:*:"
                    + prtocolinfo
                    + ":DLNA.ORG_PN=MP3;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01500000000000000000000000000000\" size=\""
                    + size
                    + "\" duration=\""
                    + duration
                    + "\">"
                    + url
                    + "</res>" + "</item>" + "</DIDL-Lite>";
        }


        Log.i(TAG, url);
        Log.i(TAG, "--------------------------");
        Log.i(TAG, "" + urlMeta);

        singer = creator;
        songName = title;


        if (urlMeta == null) {
            dmrHelper.getDmrProcessor().setURI(url, null);
            return;
        }

/*
//    Commenting as we dont need to access the image through bitmap anymore as it would be taken care by picasso after recieveing the 42 message box
        Bitmap bm = null;
        if (uri != null) {
            bm = MusicBitmap.getBitmap(uri);
        } else {
            bm = MusicBitmap.getBitmap(MAIN_CONTEXT, url);
        }
        songImage = bm;
*/

        durationSeconds = (int) ModelUtil.fromTimeString(duration);
        relTime = 0;
        /*URL=url;
        META=urlMeta;
		final Handler myhandler = new Handler();
		myhandler.postDelayed(new Runnable() {
		  @Override
		  public void run() {
		    //Do something after 100ms
			  dmrHelper.getDmrProcessor().setURI(URL, META);
		  }
		}, 500);*/


        dmrHelper.getDmrProcessor().setURI(url, urlMeta);

    }

    /*this function will give random pos for shuffle btw given range*/
    private int randomPosition(int maximum, int minimum) {
        Random rn = new Random();
        int range = maximum - minimum + 1;
        return rn.nextInt(range) + minimum;

    }

    public void playNextSong(int x) {
        Log.v(TAG, "PlayNextSong");
        if (dmsHelper == null) return;
        List<DIDLObject> adapter = dmsHelper.getDidlList();
        if (adapter == null) return;

        int totalCount = adapter.size();
        int currPosition = dmsHelper.getAdapterPosition();
        /*changed to support shuffle for local content as we we have to give random position*/
        int nextPosition;
        if (isShuffleOn) {
            if (repeatState == NowPlayingFragment.REPEAT_ONE) {
                /*assigning same position every time*/
                nextPosition = currPosition;
            } else {
                /*otherwise get random position*/
                nextPosition = randomPosition(totalCount, 0);
            }
            Log.d(TAG, "playNextSong: shuffle is on" + nextPosition);
        } else {
            /*which means shuffle is off*/
            if (repeatState == NowPlayingFragment.REPEAT_ONE) {
                /*assigning same position every time*/
                nextPosition = currPosition;
            } else if (repeatState == NowPlayingFragment.REPEAT_ALL) {
                /*if both counts are equal sending it to previous position*/
                if (totalCount == currPosition+1) {
                    nextPosition = 0;
                } else {
                    /*else increase it by x*/
                    nextPosition = currPosition + x;
                }
            } else {
                /*else increase it by x*/
                nextPosition = currPosition + x;
            }
            Log.d(TAG, "playNextSong: shuffle is off" + nextPosition);
        }

        DIDLObject object = null;

        for (int i = 0; i < totalCount; i++) {
            if (nextPosition < 0) {
                return;
//				nextPosition = totalCount - 1;
            } else if (nextPosition >= totalCount) {
                return;
//				nextPosition = 0;
            }

            object = dmsHelper.getDIDLObject(nextPosition);
            if (object instanceof Item) {
                break;
            } else {
                object = null;
            }
            nextPosition += x;
        }

        if (object == null) {
            Log.d(TAG, "PlayerObject Null" );
            return;
        }

        dmsHelper.setAdapterPosition(nextPosition);
        try {
            playSong();
        } catch (Throwable t) {
        }
    }

    @Override
    public void onUpdatePosition(long position, long duration) {
        // TODO Auto-generated method stub
        relTime = (int) position;
        if (durationSeconds == 0)
            durationSeconds = (int) duration;

    }

    @Override
    public void onUpdateVolume(int currentVolume) {
        volume = currentVolume;
    }

    public void setVolume(int currentVolume) {
        this.volume = currentVolume;
    }

    public int getVolume() {
        return volume;
    }

    @Override
    public void onPaused() {
        // TODO Auto-generated method stub
        isPlaying = false;
    }

    @Override
    public void onStoped() {
        // TODO Auto-generated method stub
        isPlaying = false;
    }

    @Override
    public void onSetURI() {
        // TODO Auto-generated method stub
    }

    @Override
    public void onPlayCompleted() {
        // TODO Auto-generated method stub
        Log.e(TAG, "On PlayCompleted-Helper");

        playNextSong(1);
    }

    @Override
    public void onPlaying() {
        // TODO Auto-generated method stub
        isPlaying = true;
    }

    @Override
    public void onActionSuccess(Action action) {

    }

    @SuppressWarnings("rawtypes")
    @Override
    public void onActionFail(String actionCallback, UpnpResponse response, String cause) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onExceptionHappend(Action actionCallback, String mTitle, String cause) {
        LibreLogger.d(this,"Exception Happend for the Title "+ mTitle + " for the cause of "+ cause );

    }

    public boolean isPlaying() {
        return isPlaying;
    }
}
