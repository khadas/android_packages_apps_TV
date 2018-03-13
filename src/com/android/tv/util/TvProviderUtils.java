/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tv.util;

import android.content.Context;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.annotation.WorkerThread;
import com.android.tv.data.Program;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** A utility class related to TvProvider. */
@RequiresApi(26)
public class TvProviderUtils {
    private static final String TAG = "TvProviderUtils";

    private static final int VERSION = 1;

    public static final String EXTRA_PROGRAM_COLUMN = Program.COLUMN_SERIES_ID;
    public static final String PREF_KEY_TV_PROVIDER_DB_CUSTOMIZATION_VERSION_CODE =
            "tv_provider_db_customization.version_code";

    private static Boolean sIsLatestVersion;

    /**
     * Updates database columns if necessary.
     *
     * @return {@code true} if it's latest or it's successfully updated. {@code false}
     * otherwise.
     */
    @WorkerThread
    public static synchronized boolean updateDbColumnsIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false;
        }
        if (sIsLatestVersion == null) {
            int version =
                    PreferenceManager.getDefaultSharedPreferences(context)
                            .getInt(PREF_KEY_TV_PROVIDER_DB_CUSTOMIZATION_VERSION_CODE, 0);
            setIsLatestVersion(version == VERSION);
        }
        if (sIsLatestVersion) {
            return true;
        }
        // TODO(b/73342889): implement add_column
        return false;
    }

    public static synchronized boolean isLatestVersion() {
        return Boolean.TRUE.equals(sIsLatestVersion)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    private static synchronized void setIsLatestVersion(boolean isLatestVersion) {
        sIsLatestVersion = isLatestVersion;
    }

    public static String[] addExtraColumnsToProjection(String[] projection) {
        List<String> projectionList = new ArrayList<>(Arrays.asList(projection));
        if (!projectionList.contains(EXTRA_PROGRAM_COLUMN)) {
            projectionList.add(EXTRA_PROGRAM_COLUMN);
        }
        projection = projectionList.toArray(projection);
        return projection;
    }
}
