package com.iwobanas.screenrecorder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.CamcorderProfile;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.iwobanas.screenrecorder.settings.Resolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static android.media.CamcorderProfile.*;

public class ResolutionsManager {

    private int width = 0;
    private int height = 0;
    private Resolution[] resolutions;
    private Resolution defaultResolution;

    private int[] standardHeights = new int[]{1080, 720, 480, 360, 240};
    private int[] standardWidths = new int[]{1080, 720, 480};

    private String original = "Max";
    private String half = "Half";

    @SuppressLint("NewApi")
    public ResolutionsManager(Context context) {
        Display display = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        height = Math.min(metrics.heightPixels, metrics.widthPixels);
        width = Math.max(metrics.heightPixels, metrics.widthPixels);

        generateResolutions();
    }

    private void generateResolutions() {
        double aspectRatio = (double) width / (double) height;

        Map<Integer, Resolution> resolutionsByHeight = new HashMap<Integer, Resolution>();

        ArrayList<Resolution> resolutions = new ArrayList<Resolution>(10);

        for (int i = 0; i < standardHeights.length; i++) {
            int h = standardHeights[i];
            int w = nearestEven(h * aspectRatio);
            String label = null;

            if (h > height)
                continue;

            if (h == height) {
                w = width;
                label = original;
            } else if (h == height / 2) {
                label = half;
            } else {
                label = h + "p";
            }
            Resolution resolution = new Resolution(label, w, h);
            resolutionsByHeight.put(h, resolution);
            if (h == 720) {
                defaultResolution = resolution;
            }
            resolutions.add(resolution);
        }

        for (int i = 0; i < standardWidths.length; i++) {
            int w = standardWidths[i];
            int h = nearestEven(w / aspectRatio);
            String label = w + "p↦";

            if (h > height || resolutionsByHeight.containsKey(h))
                continue;

            resolutions.add(new Resolution(label, w, h));
        }

        if (!resolutionsByHeight.containsKey(height)) {
            Resolution resolution = new Resolution(original, width, height);
            resolutionsByHeight.put(resolution.getHeight(), resolution);
            resolutions.add(resolution);
        }
        if (!resolutionsByHeight.containsKey(nearestEven(height / 2.0))) {
            Resolution resolution = new Resolution(half, nearestEven(width / 2.0), nearestEven(height / 2.0));
            resolutionsByHeight.put(resolution.getHeight(), resolution);
            resolutions.add(resolution);
        }

        if (defaultResolution == null) {
            defaultResolution = resolutionsByHeight.get(height);
        }

        addStandardResolutions(aspectRatio, resolutions, resolutionsByHeight);

        Collections.sort(resolutions, new Comparator<Resolution>() {
            @Override
            public int compare(Resolution a, Resolution b) {
                return b.getHeight() - a.getHeight();
            }
        });

        this.resolutions = resolutions.toArray(new Resolution[resolutions.size()]);
    }

    private void addStandardResolutions(double aspectRatio, ArrayList<Resolution> resolutions, Map<Integer, Resolution> resolutionsByHeight) {
        for (int profileId : new int[]{QUALITY_1080P, QUALITY_720P, CamcorderProfile.QUALITY_480P}) {
            if (!CamcorderProfile.hasProfile(profileId)) continue;
            CamcorderProfile profile = CamcorderProfile.get(profileId);
            if (profile.videoFrameHeight > height && profile.videoFrameWidth > width) {
                continue;
            }
            Resolution existingResolution = resolutionsByHeight.get(profile.videoFrameHeight);
            if (existingResolution != null && existingResolution.getWidth() == profile.videoFrameWidth) {
                continue;
            }

            int paddingHeight = 0;
            int paddingWidth = 0;

            if (profile.videoFrameHeight * aspectRatio > profile.videoFrameWidth) {
                paddingHeight = (int) Math.round((profile.videoFrameHeight - profile.videoFrameWidth / aspectRatio) / 2);
            } else {
                paddingWidth = (int) Math.round((profile.videoFrameWidth - profile.videoFrameHeight * aspectRatio) / 2);
            }
            resolutions.add(new Resolution(profile.videoFrameHeight + "p",
                    profile.videoFrameWidth, profile.videoFrameHeight,
                    paddingWidth, paddingHeight));
        }
    }

    private int nearestEven(double value) {
        return (int) (2.0 * Math.round(value / 2.0));
    }

    public Resolution getDefaultResolution() {
        return defaultResolution;
    }

    public Resolution[] getResolutions() {
        return resolutions;
    }

    public Resolution getResolution(int width, int height) {
        for (Resolution resolution : resolutions) {
            if (resolution.getWidth() == width && resolution.getHeight() == height) {
                return resolution;
            }
        }
        return null;
    }
}
