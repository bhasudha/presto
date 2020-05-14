/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.hive.util;

import com.facebook.presto.spi.PrestoException;
import com.google.common.collect.ImmutableList;
import org.apache.hadoop.mapred.FileSplit;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.facebook.presto.hive.HiveErrorCode.HIVE_UNSUPPORTED_FORMAT;

/**
 * Utility class for both extracting customSplitInfo Map from a custom FileSplit and transforming the customSplitInfo back into a FileSplit.
 */
public class CustomSplitConversionUtils
{
    private static final List<CustomSplitConverter> converters = ImmutableList.of(new HudiRealtimeSplitConverter());

    private CustomSplitConversionUtils()
    {
    }

    public static Map<String, String> extractAnyCustomSplitInfo(FileSplit split)
    {
        Optional<Map<String, String>> customSplitData;
        for (CustomSplitConverter converter : converters) {
            customSplitData = converter.extractAnyCustomSplitInfo(split);
            if (customSplitData.isPresent()) {
                return customSplitData.get();
            }
        }
        return Collections.emptyMap();
    }

    public static FileSplit recreateSplitWithCustomInfo(FileSplit split, Map<String, String> customSplitInfo)
    {
        Optional<FileSplit> fileSplit;
        for (CustomSplitConverter converter : converters) {
            try {
                fileSplit = converter.recreateSplitWithCustomInfo(split, customSplitInfo);
            }
            catch (IOException e) {
                throw new PrestoException(HIVE_UNSUPPORTED_FORMAT, "Split converter : " + converter.getClass() + " failed to create fileSplit.", e);
            }
            if (fileSplit.isPresent()) {
                return fileSplit.get();
            }
        }
        return split;
    }
}