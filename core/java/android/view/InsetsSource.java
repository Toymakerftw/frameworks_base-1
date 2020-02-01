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

package android.view;

import static android.view.InsetsState.ITYPE_IME;

import android.annotation.Nullable;
import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.InsetsState.InternalInsetsType;

import java.io.PrintWriter;
import java.util.Objects;

/**
 * Represents the state of a single window generating insets for clients.
 * @hide
 */
public class InsetsSource implements Parcelable {

    private final @InternalInsetsType int mType;

    /** Frame of the source in screen coordinate space */
    private final Rect mFrame;
    private @Nullable Rect mVisibleFrame;
    private boolean mVisible;

    private final Rect mTmpFrame = new Rect();

    public InsetsSource(@InternalInsetsType int type) {
        mType = type;
        mFrame = new Rect();
        mVisible = InsetsState.getDefaultVisibility(type);
    }

    public InsetsSource(InsetsSource other) {
        mType = other.mType;
        mFrame = new Rect(other.mFrame);
        mVisible = other.mVisible;
        mVisibleFrame = other.mVisibleFrame != null
                ? new Rect(other.mVisibleFrame)
                : null;
    }

    public void setFrame(Rect frame) {
        mFrame.set(frame);
    }

    public void setVisibleFrame(@Nullable Rect visibleFrame) {
        mVisibleFrame = visibleFrame != null ? new Rect(visibleFrame) : visibleFrame;
    }

    public void setVisible(boolean visible) {
        mVisible = visible;
    }

    public @InternalInsetsType int getType() {
        return mType;
    }

    public Rect getFrame() {
        return mFrame;
    }

    public @Nullable Rect getVisibleFrame() {
        return mVisibleFrame;
    }

    public boolean isVisible() {
        return mVisible;
    }

    /**
     * Calculates the insets this source will cause to a client window.
     *
     * @param relativeFrame The frame to calculate the insets relative to.
     * @param ignoreVisibility If true, always reports back insets even if source isn't visible.
     * @return The resulting insets. The contract is that only one side will be occupied by a
     *         source.
     */
    public Insets calculateInsets(Rect relativeFrame, boolean ignoreVisibility) {
        return calculateInsets(relativeFrame, mFrame, ignoreVisibility);
    }

    /**
     * Like {@link #calculateInsets(Rect, boolean)}, but will return visible insets.
     */
    public Insets calculateVisibleInsets(Rect relativeFrame) {
        return calculateInsets(relativeFrame, mVisibleFrame != null ? mVisibleFrame : mFrame,
                false /* ignoreVisibility */);
    }

    private Insets calculateInsets(Rect relativeFrame, Rect frame, boolean ignoreVisibility) {
        if (!ignoreVisibility && !mVisible) {
            return Insets.NONE;
        }
        if (!mTmpFrame.setIntersect(frame, relativeFrame)) {
            return Insets.NONE;
        }

        // TODO: Currently, non-floating IME always intersects at bottom due to issues with cutout.
        // However, we should let the policy decide from the server.
        if (getType() == ITYPE_IME) {
            return Insets.of(0, 0, 0, mTmpFrame.height());
        }

        // Intersecting at top/bottom
        if (mTmpFrame.width() == relativeFrame.width()) {
            if (mTmpFrame.top == relativeFrame.top) {
                return Insets.of(0, mTmpFrame.height(), 0, 0);
            } else {
                return Insets.of(0, 0, 0, mTmpFrame.height());
            }
        }
        // Intersecting at left/right
        else if (mTmpFrame.height() == relativeFrame.height()) {
            if (mTmpFrame.left == relativeFrame.left) {
                return Insets.of(mTmpFrame.width(), 0, 0, 0);
            } else {
                return Insets.of(0, 0, mTmpFrame.width(), 0);
            }
        } else {
            return Insets.NONE;
        }
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("InsetsSource type="); pw.print(InsetsState.typeToString(mType));
        pw.print(" frame="); pw.print(mFrame.toShortString());
        if (mVisibleFrame != null) {
            pw.print(" visibleFrmae="); pw.print(mVisibleFrame.toShortString());
        }
        pw.print(" visible="); pw.print(mVisible);
        pw.println();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InsetsSource that = (InsetsSource) o;

        if (mType != that.mType) return false;
        if (mVisible != that.mVisible) return false;
        if (!Objects.equals(mVisibleFrame, that.mVisibleFrame)) return false;
        return mFrame.equals(that.mFrame);
    }

    @Override
    public int hashCode() {
        int result = mType;
        result = 31 * result + mFrame.hashCode();
        result = 31 * result + (mVisibleFrame != null ? mVisibleFrame.hashCode() : 0);
        result = 31 * result + (mVisible ? 1 : 0);
        return result;
    }

    public InsetsSource(Parcel in) {
        mType = in.readInt();
        mFrame = in.readParcelable(null /* loader */);
        mVisibleFrame = in.readParcelable(null /* loader */);
        mVisible = in.readBoolean();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mType);
        dest.writeParcelable(mFrame, 0 /* flags*/);
        dest.writeParcelable(mVisibleFrame, 0 /* flags */);
        dest.writeBoolean(mVisible);
    }

    public static final @android.annotation.NonNull Creator<InsetsSource> CREATOR = new Creator<InsetsSource>() {

        public InsetsSource createFromParcel(Parcel in) {
            return new InsetsSource(in);
        }

        public InsetsSource[] newArray(int size) {
            return new InsetsSource[size];
        }
    };
}
