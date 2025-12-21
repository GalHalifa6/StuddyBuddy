import React, { useEffect, useRef } from 'react';

interface JitsiMeetEmbedProps {
  roomName: string;
  displayName?: string;
  onParticipantLeft?: () => void;
  onParticipantJoined?: () => void;
  config?: {
    startWithAudioMuted?: boolean;
    startWithVideoMuted?: boolean;
    enableWelcomePage?: boolean;
    enableClosePage?: boolean;
  };
  style?: React.CSSProperties;
  className?: string;
}

/**
 * Jitsi Meet Embed Component for Web
 * Embeds a Jitsi Meet video conference in an iframe
 */
export const JitsiMeetEmbed: React.FC<JitsiMeetEmbedProps> = ({
  roomName,
  displayName,
  config = {},
  style,
  className,
}) => {
  const iframeRef = useRef<HTMLIFrameElement>(null);

  useEffect(() => {
    if (!iframeRef.current) return;

    // Extract room name from full URL if needed
    const cleanRoomName = roomName.includes('meet.jit.si/') 
      ? roomName.split('meet.jit.si/')[1] 
      : roomName;

    // Build Jitsi Meet URL with configuration
    const params = new URLSearchParams({
      room: cleanRoomName,
      ...(displayName && { userInfo: JSON.stringify({ displayName }) }),
      ...(config.startWithAudioMuted !== undefined && { 
        'config.startWithAudioMuted': String(config.startWithAudioMuted) 
      }),
      ...(config.startWithVideoMuted !== undefined && { 
        'config.startWithVideoMuted': String(config.startWithVideoMuted) 
      }),
      ...(config.enableWelcomePage !== undefined && { 
        'config.enableWelcomePage': String(config.enableWelcomePage) 
      }),
      ...(config.enableClosePage !== undefined && { 
        'config.enableClosePage': String(config.enableClosePage) 
      }),
    });

    const jitsiUrl = `https://meet.jit.si/${cleanRoomName}?${params.toString()}`;

    // Set iframe source
    if (iframeRef.current) {
      iframeRef.current.src = jitsiUrl;
    }
  }, [roomName, displayName, config]);

  return (
    <iframe
      ref={iframeRef}
      allow="camera; microphone; fullscreen; speaker; display-capture"
      style={{
        width: '100%',
        height: '100%',
        border: 0,
        ...style,
      }}
      className={className}
      title="Jitsi Meet"
    />
  );
};

