/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.sprd.fileexplore.file;

import android.util.Log;
import com.sprd.fileexplore.util.FileUtil;

import java.text.CollationKey;
import java.text.Collator;
import java.text.RuleBasedCollator;
import java.util.Comparator;


public final class FileInfoComparator implements Comparator<FileInfo> {
    private static final String TAG = "FileInfoComparator";
    /** Create a instance comparator. */
    private static FileInfoComparator sInstance = new FileInfoComparator();

    public static final String SORT_KEY = "sort_key";

    public static final int SORT_BY_NAME = 0;
    public static final int SORT_BY_TYPE = 1;
    public static final int SORT_BY_TIME = 2;
    public static final int SORT_BY_SIZE = 3;

    public static final int SORT_BY_NAME_DESC = 4;
    public static final int SORT_BY_TYPE_DESC = 5;
    public static final int SORT_BY_TIME_DESC = 6;
    public static final int SORT_BY_SIZE_DESC = 7;


    public static int getSelectItemByType(int type){
        switch(type){

            case SORT_BY_NAME:
            case SORT_BY_NAME_DESC:
                return 0;
            case SORT_BY_TYPE:
            case SORT_BY_TYPE_DESC:
                return 1;
            case SORT_BY_TIME_DESC:
            case SORT_BY_TIME:
                return 2;
            case SORT_BY_SIZE:
            case SORT_BY_SIZE_DESC:
                return 3;
            default:
                return 0;
        }
    }

    private RuleBasedCollator mCollator = null;

    private int mSortType = SORT_BY_NAME;

    /**
     * Constructor for FileInfoComparator class.
     */
    private FileInfoComparator() {
    }


    public int getSortType(){
        return mSortType;
    }
    /**
     * This method set the sort mode. 0 means by type, 1 means by name, 2 means by size, 3 means by
     * time.
     *
     * @param sort sort mode.
     */
    private void setSortType(int sort) {
        mSortType = sort;
        if (mCollator == null) {
            mCollator = (RuleBasedCollator) Collator.getInstance(java.util.Locale.CHINA);
        }
    }

    /**
     * This method get instance of FileInfoComparator.
     *
     * @param sort sort mode.
     * @return a instance of FileInfoComparator.
     */
    public static FileInfoComparator getInstance(int sort) {
        sInstance.setSortType(sort);
        return sInstance;
    }
    /**
     * This method get instance of FileInfoComparator.
     *
     * @return a instance of FileInfoComparator.
     */
    public static FileInfoComparator getInstance() {
        return sInstance;
    }
    /**
     * This method compares the files based on the order: category folders->common folders->files
     *
     * @param op the first file
     * @param oq the second file
     * @return a negative integer, zero, or a positive integer as the first file is smaller than,
     *         equal to, or greater than the second file, ignoring case considerations.
     */
    @Override
    public int compare(FileInfo op, FileInfo oq) {
        // if only one is directory
        boolean isOpDirectory = op.isFolder();
        boolean isOqDirectory = oq.isFolder();
        if (isOpDirectory ^ isOqDirectory) {
            // one is a folder and one is not a folder
            Log.v(TAG, op.getName() + " vs " + oq.getName() + " result="
                    + (isOpDirectory ? -1 : 1));
            return isOpDirectory ? -1 : 1;
        }

        switch (mSortType) {
        case SORT_BY_TYPE:
            return sortByType(op, oq);
        case SORT_BY_TYPE_DESC:
            return - sortByType(op, oq);
        case SORT_BY_NAME:
            return sortByName(op, oq);
        case SORT_BY_NAME_DESC:
            return - sortByName(op, oq);
        case SORT_BY_SIZE:
            return sortBySize(op, oq);
        case SORT_BY_SIZE_DESC:
            return - sortBySize(op, oq);
        case SORT_BY_TIME:
            return -sortByTime(op, oq);
        case SORT_BY_TIME_DESC:
            return sortByTime(op, oq);
        default:
            return sortByName(op, oq);
        }
    }

    /**
     * This method compares the files based on their type
     *
     * @param op the first file
     * @param oq the second file
     * @return a negative integer, zero, or a positive integer as the first file is smaller than,
     *         equal to, or greater than the second file, ignoring case considerations.
     */
    private int sortByType(FileInfo op, FileInfo oq) {
        boolean isOpDirectory = op.isFolder();
        boolean isOqDirectory = oq.isFolder();

        if (!isOpDirectory && !isOqDirectory) {
            // both are not directory
            String opExtension = FileUtil.getFileExtension(op.getName());
            String oqExtension = FileUtil.getFileExtension(oq.getName());
            if (opExtension == null && oqExtension != null) {
                return -1;
            } else if (opExtension != null && oqExtension == null) {
                return 1;
            } else if (opExtension != null && oqExtension != null) {
                if (!opExtension.equalsIgnoreCase(oqExtension)) {
                    return opExtension.compareToIgnoreCase(oqExtension);
                }
            }
        }
        return sortByName(op, oq);
    }

    /**
     * This method compares the files based on their names.
     *
     * @param op the first file
     * @param oq the second file
     * @return a negative integer, zero, or a positive integer as the first file is smaller than,
     *         equal to, or greater than the second file, ignoring case considerations.
     */
    private int sortByName(FileInfo op, FileInfo oq) {
        CollationKey c1 = mCollator.getCollationKey(op.getName());
        CollationKey c2 = mCollator.getCollationKey(oq.getName());
        return mCollator.compare(c1.getSourceString(), c2.getSourceString());
    }

    /**
     * This method compares the files based on their sizes
     *
     * @param op the first file
     * @param oq the second file
     * @return a negative integer, zero, or a positive integer as the first file is smaller than,
     *         equal to, or greater than the second file, ignoring case considerations.
     */
    private int sortBySize(FileInfo op, FileInfo oq) {
        if (!op.isFolder() && !oq.isFolder()) {
            long opSize = op.getFileSize();
            long oqSize = oq.getFileSize();
            if (opSize != oqSize) {
                return opSize > oqSize ? -1 : 1;
            }
        }
        return sortByName(op, oq);
    }

    /**
     * This method compares the files based on their modified time
     *
     * @param op the first file
     * @param oq the second file
     * @return a negative integer, zero, or a positive integer as the first file is smaller than,
     *         equal to, or greater than the second file, ignoring case considerations.
     */
    private int sortByTime(FileInfo op, FileInfo oq) {
        long opTime = op.getLastModifiedTime();
        long oqTime = oq.getLastModifiedTime();
        if (opTime != oqTime) {
            return opTime > oqTime ? -1 : 1;
        }
        return sortByName(op, oq);
    }
}