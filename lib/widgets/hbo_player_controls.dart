import 'dart:async';
import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';
import 'package:volume_controller/volume_controller.dart';
import 'package:screen_brightness/screen_brightness.dart';

class HboPlayerControls extends StatefulWidget {
  final VideoPlayerController controller;
  final String title;
  final VoidCallback onSettingsTap;

  const HboPlayerControls({
    Key? key,
    required this.controller,
    required this.title,
    required this.onSettingsTap,
  }) : super(key: key);

  @override
  State<HboPlayerControls> createState() => _HboPlayerControlsState();
}

class _HboPlayerControlsState extends State<HboPlayerControls> {
  bool _showControls = true;
  Timer? _hideTimer;
  double _volume = 0.5;
  double _brightness = 0.5;
  
  bool _isDraggingSeek = false;
  double _seekDragValue = 0;

  @override
  void initState() {
    super.initState();
    _startHideTimer();
    _initVolumeAndBrightness();
    widget.controller.addListener(_updateState);
  }

  Future<void> _initVolumeAndBrightness() async {
    VolumeController().listener((volume) {
      if(mounted) setState(() => _volume = volume);
    });
    try {
      _brightness = await ScreenBrightness().current;
    } catch (e) {
      // ignore
    }
  }

  void _updateState() {
    if (mounted && !_isDraggingSeek) {
      setState(() {});
    }
  }

  void _startHideTimer() {
    _hideTimer?.cancel();
    _hideTimer = Timer(const Duration(seconds: 4), () {
      if (mounted && widget.controller.value.isPlaying) {
        setState(() => _showControls = false);
      }
    });
  }

  void _toggleControls() {
    setState(() {
      _showControls = !_showControls;
    });
    if (_showControls) _startHideTimer();
  }

  @override
  void dispose() {
    _hideTimer?.cancel();
    VolumeController().removeListener();
    widget.controller.removeListener(_updateState);
    super.dispose();
  }

  String _formatDuration(Duration duration) {
    String twoDigits(int n) => n.toString().padLeft(2, "0");
    String twoDigitMinutes = twoDigits(duration.inMinutes.remainder(60));
    String twoDigitSeconds = twoDigits(duration.inSeconds.remainder(60));
    if (duration.inHours > 0) {
      return "${duration.inHours}:$twoDigitMinutes:$twoDigitSeconds";
    }
    return "$twoDigitMinutes:$twoDigitSeconds";
  }

  void _seekRelative(Duration offset) {
    final current = widget.controller.value.position;
    widget.controller.seekTo(current + offset);
    _startHideTimer();
  }

  void _handleVerticalDrag(DragUpdateDetails details, bool isLeftSide) async {
    _startHideTimer();
    double delta = -details.primaryDelta! / 300;
    if (isLeftSide) {
      // Brillo
      _brightness = (_brightness + delta).clamp(0.0, 1.0);
      try {
        await ScreenBrightness().setScreenBrightness(_brightness);
      } catch (e) {}
      setState(() {});
    } else {
      // Volumen
      _volume = (_volume + delta).clamp(0.0, 1.0);
      VolumeController().setVolume(_volume);
      setState(() {});
    }
  }

  @override
  Widget build(BuildContext context) {
    final position = widget.controller.value.position;
    final duration = widget.controller.value.duration;

    return GestureDetector(
      onTap: _toggleControls,
      onDoubleTapDown: (details) {
        final screenWidth = MediaQuery.of(context).size.width;
        if (details.globalPosition.dx < screenWidth / 2) {
          _seekRelative(const Duration(seconds: -10));
        } else {
          _seekRelative(const Duration(seconds: 10));
        }
      },
      child: Stack(
        children: [
          // Gestos de swipe (Brillo / Volumen)
          if (!_showControls)
            Row(
              children: [
                Expanded(
                  child: GestureDetector(
                    onVerticalDragUpdate: (details) => _handleVerticalDrag(details, true),
                    child: Container(color: Colors.transparent),
                  ),
                ),
                Expanded(
                  child: GestureDetector(
                    onVerticalDragUpdate: (details) => _handleVerticalDrag(details, false),
                    child: Container(color: Colors.transparent),
                  ),
                ),
              ],
            ),

          // Controles visuales
          AnimatedOpacity(
            opacity: _showControls ? 1.0 : 0.0,
            duration: const Duration(milliseconds: 300),
            child: Container(
              color: Colors.black.withOpacity(0.6),
              child: Stack(
                children: [
                  // Top Bar
                  Positioned(
                    top: 20,
                    left: 20,
                    right: 20,
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Row(
                          children: [
                            IconButton(
                              icon: const Icon(Icons.arrow_back_ios, color: Colors.white),
                              onPressed: () => Navigator.of(context).pop(),
                            ),
                            Text(
                              widget.title,
                              style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
                            ),
                          ],
                        ),
                        IconButton(
                          icon: const Icon(Icons.settings, color: Colors.white),
                          onPressed: widget.onSettingsTap,
                        ),
                      ],
                    ),
                  ),

                  // Center Play/Pause & Seek
                  Center(
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        IconButton(
                          icon: const Icon(Icons.replay_10, color: Colors.white, size: 40),
                          onPressed: () => _seekRelative(const Duration(seconds: -10)),
                        ),
                        const SizedBox(width: 40),
                        GestureDetector(
                          onTap: () {
                            setState(() {
                              if (widget.controller.value.isPlaying) {
                                widget.controller.pause();
                              } else {
                                widget.controller.play();
                              }
                            });
                            _startHideTimer();
                          },
                          child: Container(
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              color: Colors.white.withOpacity(0.2),
                            ),
                            padding: const EdgeInsets.all(16),
                            child: Icon(
                              widget.controller.value.isPlaying ? Icons.pause : Icons.play_arrow,
                              color: Colors.white,
                              size: 50,
                            ),
                          ),
                        ),
                        const SizedBox(width: 40),
                        IconButton(
                          icon: const Icon(Icons.forward_10, color: Colors.white, size: 40),
                          onPressed: () => _seekRelative(const Duration(seconds: 10)),
                        ),
                      ],
                    ),
                  ),

                  // Bottom Bar (Progress)
                  Positioned(
                    bottom: 30,
                    left: 20,
                    right: 20,
                    child: Column(
                      children: [
                        SliderTheme(
                          data: SliderThemeData(
                            trackHeight: 4,
                            thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 6),
                            overlayShape: const RoundSliderOverlayShape(overlayRadius: 14),
                            activeTrackColor: Colors.purpleAccent,
                            inactiveTrackColor: Colors.white24,
                            thumbColor: Colors.white,
                          ),
                          child: Slider(
                            value: _isDraggingSeek 
                                ? _seekDragValue 
                                : position.inMilliseconds.toDouble(),
                            min: 0,
                            max: duration.inMilliseconds.toDouble() > 0 ? duration.inMilliseconds.toDouble() : 1,
                            onChangeStart: (value) {
                              setState(() {
                                _isDraggingSeek = true;
                                _startHideTimer();
                              });
                            },
                            onChanged: (value) {
                              setState(() {
                                _seekDragValue = value;
                              });
                            },
                            onChangeEnd: (value) {
                              setState(() {
                                _isDraggingSeek = false;
                                widget.controller.seekTo(Duration(milliseconds: value.toInt()));
                                _startHideTimer();
                              });
                            },
                          ),
                        ),
                        Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 16),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text(
                                _formatDuration(_isDraggingSeek ? Duration(milliseconds: _seekDragValue.toInt()) : position),
                                style: const TextStyle(color: Colors.white),
                              ),
                              Text(
                                _formatDuration(duration),
                                style: const TextStyle(color: Colors.white70),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
