#include <jni.h>
#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.Foundation.Collections.h>
#include <winrt/Windows.Media.Control.h>
#include <winrt/Windows.Storage.Streams.h>
#include <algorithm>
#include <string>
#include <vector>
#include <thread>
#include <chrono>

using namespace winrt;
using namespace Windows::Media;
using namespace Windows::Media::Control;
using namespace Windows::Storage::Streams;

// All JNI entrypoints run on the single Java media worker, never the render thread.
static GlobalSystemMediaTransportControlsSessionManager manager{nullptr};
static GlobalSystemMediaTransportControlsSession session{nullptr};
static std::wstring sourceId, artworkKey;
static std::vector<uint8_t> artwork;
static bool apartment = false, repeatUnavailable = false;

static std::wstring quote(std::wstring const& value) {
    std::wstring out=L"\"";
    for(auto c:value) {
        switch(c) {
            case L'\\':out+=L"\\\\";break;case L'\"':out+=L"\\\"";break;
            case L'\n':out+=L"\\n";break;case L'\r':out+=L"\\r";break;case L'\t':out+=L"\\t";break;
            default: if(c<32){wchar_t escaped[7];swprintf_s(escaped,L"\\u%04x",c);out+=escaped;}else out+=c;
        }
    }
    return out+L"\"";
}
static jstring string(JNIEnv* env,std::wstring const& value){return env->NewString(reinterpret_cast<jchar const*>(value.data()),static_cast<jsize>(value.size()));}
static void selectSpotify() {
    auto found=GlobalSystemMediaTransportControlsSession{nullptr};
    for(auto const& candidate:manager.GetSessions()) {
        std::wstring id=candidate.SourceAppUserModelId().c_str(),lower=id;
        std::transform(lower.begin(),lower.end(),lower.begin(),::towlower);
        if(lower.find(L"spotify")!=std::wstring::npos) {
            found=candidate;
            if(id==sourceId || candidate.GetPlaybackInfo().PlaybackStatus()==GlobalSystemMediaTransportControlsSessionPlaybackStatus::Playing)break;
        }
    }
    if(!found){session=nullptr;sourceId.clear();artworkKey.clear();artwork.clear();repeatUnavailable=false;return;}
    std::wstring id=found.SourceAppUserModelId().c_str();
    if(id!=sourceId){artworkKey.clear();artwork.clear();repeatUnavailable=false;}
    sourceId=id;session=found;
}
extern "C" JNIEXPORT void JNICALL Java_dev_fluxtweaks_media_WindowsMedia_open(JNIEnv* env,jclass) {
    try {
        if(!apartment){init_apartment(apartment_type::multi_threaded);apartment=true;}
        manager=GlobalSystemMediaTransportControlsSessionManager::RequestAsync().get();repeatUnavailable=false;
    }catch(hresult_error const& ex){env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),to_string(ex.message()).c_str());}
}
extern "C" JNIEXPORT jstring JNICALL Java_dev_fluxtweaks_media_WindowsMedia_snapshot(JNIEnv* env,jclass) {
    try {
        if(!manager)return string(env,L"{\"available\":false}");
        selectSpotify();if(!session)return string(env,L"{\"available\":false}");
        auto properties=session.TryGetMediaPropertiesAsync().get();
        auto playback=session.GetPlaybackInfo();auto controls=playback.Controls();auto timeline=session.GetTimelineProperties();
        std::wstring title=properties.Title().c_str(),artist=properties.Artist().c_str();
        std::wstring key=sourceId+L"\n"+title+L"\n"+artist+L"\n"+properties.AlbumTitle().c_str();
        if(key!=artworkKey) {
            artwork.clear();auto thumb=properties.Thumbnail();
            if(thumb) {
                auto stream=thumb.OpenReadAsync().get();
                if(stream.Size()>0 && stream.Size()<=4*1024*1024) {
                    auto reader=DataReader(stream);auto length=static_cast<uint32_t>(stream.Size());
                    reader.LoadAsync(length).get();artwork.resize(length);reader.ReadBytes(artwork);reader.Close();
                }
                stream.Close();
            }
            artworkKey=key;
        }
        auto duration=std::max<int64_t>(0,(timeline.EndTime()-timeline.StartTime()).count()/10000);
        auto position=std::clamp<int64_t>((timeline.Position()-timeline.StartTime()).count()/10000,0,duration);
        bool playing=playback.PlaybackStatus()==GlobalSystemMediaTransportControlsSessionPlaybackStatus::Playing;
        // Windows reports the position at LastUpdatedTime, which may predate this poll.
        if(playing && timeline.LastUpdatedTime().time_since_epoch().count()>0) {
            auto elapsed=std::clamp<int64_t>((winrt::clock::now()-timeline.LastUpdatedTime()).count()/10000,0,duration);
            position=std::min<int64_t>(duration,position+elapsed);
        }
        auto repeat=playback.AutoRepeatMode();int mode=repeat?static_cast<int>(repeat.Value()):0;
        auto yes=[](bool value){return value?L"true":L"false";};
        std::wstring json=L"{\"available\":true,\"title\":"+quote(title)+L",\"artist\":"+quote(artist)+L",\"artworkKey\":"+quote(artworkKey)
            +L",\"playing\":"+yes(playing)+L",\"position\":"+std::to_wstring(position)+L",\"duration\":"+std::to_wstring(duration)
            +L",\"previous\":"+yes(controls.IsPreviousEnabled())+L",\"next\":"+yes(controls.IsNextEnabled())
            +L",\"toggle\":"+yes(controls.IsPlayPauseToggleEnabled()||(playing?controls.IsPauseEnabled():controls.IsPlayEnabled()))
            +L",\"repeatSupported\":"+yes(controls.IsRepeatEnabled()&&static_cast<bool>(repeat)&&!repeatUnavailable)+L",\"repeat\":"+std::to_wstring(mode)+L"}";
        return string(env,json);
    }catch(...){session=nullptr;return string(env,L"{\"available\":false}");}
}
extern "C" JNIEXPORT jbyteArray JNICALL Java_dev_fluxtweaks_media_WindowsMedia_artwork(JNIEnv* env,jclass) {
    auto result=env->NewByteArray(static_cast<jsize>(artwork.size()));
    if(result&&!artwork.empty())env->SetByteArrayRegion(result,0,static_cast<jsize>(artwork.size()),reinterpret_cast<jbyte const*>(artwork.data()));
    return result;
}
extern "C" JNIEXPORT jboolean JNICALL Java_dev_fluxtweaks_media_WindowsMedia_command(JNIEnv*,jclass,jint command) {
    try {
        if(!session)return false;auto playback=session.GetPlaybackInfo();auto controls=playback.Controls();
        switch(command) {
            case 0:if(controls.IsPlayPauseToggleEnabled())return session.TryTogglePlayPauseAsync().get();
                if(playback.PlaybackStatus()==GlobalSystemMediaTransportControlsSessionPlaybackStatus::Playing)return controls.IsPauseEnabled()&&session.TryPauseAsync().get();
                return controls.IsPlayEnabled()&&session.TryPlayAsync().get();
            case 1:return controls.IsPreviousEnabled()&&session.TrySkipPreviousAsync().get();
            case 2:return controls.IsNextEnabled()&&session.TrySkipNextAsync().get();
            case 3:{
                auto old=playback.AutoRepeatMode();if(!controls.IsRepeatEnabled()||!old||repeatUnavailable)return false;
                int mode=static_cast<int>(old.Value());auto requested=static_cast<MediaPlaybackAutoRepeatMode>(mode==0?2:mode==2?1:0);
                if(session.TryChangeAutoRepeatModeAsync(requested).get()) {
                    // Some Spotify versions acknowledge Windows repeat requests without reporting a change.
                    for(int i=0;i<20;i++) {
                        std::this_thread::sleep_for(std::chrono::milliseconds(100));auto updated=session.GetPlaybackInfo().AutoRepeatMode();
                        if(updated&&updated.Value()==requested)return true;
                    }
                }
                repeatUnavailable=true;return false;
            }
            default:return false;
        }
    }catch(...){return false;}
}
extern "C" JNIEXPORT void JNICALL Java_dev_fluxtweaks_media_WindowsMedia_close(JNIEnv*,jclass) {
    session=nullptr;manager=nullptr;artwork.clear();sourceId.clear();artworkKey.clear();
    if(apartment){uninit_apartment();apartment=false;}
}

