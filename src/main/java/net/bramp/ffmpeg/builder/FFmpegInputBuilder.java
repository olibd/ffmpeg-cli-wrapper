package net.bramp.ffmpeg.builder;

import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import net.bramp.ffmpeg.FFmpegUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by olivier on 16-12-31.
 */
public class FFmpegInputBuilder {

    FFmpegBuilder parent;

    // Input settings
    String format;
    Long startOffset; // in millis
    final List<String> inputs = new ArrayList<>();
    final Map<String, FFmpegProbeResult> inputProbes = new TreeMap<>();
    boolean read_at_native_frame_rate = false;

    final List<String> extra_args = new ArrayList<>();

    protected FFmpegInputBuilder(FFmpegBuilder parent, String inputFormat, String inputFile) {
        this.parent = checkNotNull(parent);
        format = inputFormat;
        addInput(checkNotNull(inputFile));
    }

    protected FFmpegInputBuilder(FFmpegBuilder parent, String inputFormat, FFmpegProbeResult inputFile) {
        this.parent = checkNotNull(parent);
        format = inputFormat;
        addInput(checkNotNull(inputFile));
    }

    public FFmpegInputBuilder addInput(FFmpegProbeResult result) {
        checkNotNull(result);
        String filename = checkNotNull(result.format).filename;
        inputProbes.put(filename, result);
        return addInput(filename);
    }

    public FFmpegInputBuilder addInput(String filename) {
        checkNotNull(filename);
        inputs.add(filename);
        return this;
    }

    public FFmpegInputBuilder readAtNativeFrameRate() {
        this.read_at_native_frame_rate = true;
        return this;
    }

    protected void clearInputs() {
        inputs.clear();
        inputProbes.clear();
    }

    public FFmpegInputBuilder setInput(FFmpegProbeResult result) {
        checkNotNull(result);
        clearInputs();
        return addInput(result);
    }

    public FFmpegInputBuilder setInput(String filename) {
        checkNotNull(filename);
        clearInputs();
        return addInput(filename);
    }

    public FFmpegInputBuilder setFormat(String format) {
        this.format = checkNotNull(format);
        return this;
    }

    public FFmpegInputBuilder setStartOffset(long duration, TimeUnit units) {
        checkNotNull(duration);
        checkNotNull(units);

        this.startOffset = units.toMillis(duration);

        return this;
    }

    /**
     * Add additional ouput arguments (for flags which aren't currently supported).
     *
     * @param values
     */
    public FFmpegInputBuilder addExtraArgs(String... values) {
        checkArgument(values.length > 0, "One or more values must be supplied");
        for (String value : values) {
            extra_args.add(checkNotNull(value));
        }
        return this;
    }

    /**
     * Finished with this output
     *
     * @return the parent FFmpegBuilder
     */
    public FFmpegBuilder done() {
        Preconditions.checkState(parent != null, "Can not call done without parent being set");
        return parent;
    }

    public List<String> build() {
        Preconditions.checkState(parent != null, "Can not build without parent being set");

        ImmutableList.Builder<String> args = new ImmutableList.Builder<String>();

        Preconditions.checkArgument(!inputs.isEmpty(), "At least one input must be specified");

        if (startOffset != null) {
            args.add("-ss").add(FFmpegUtils.millisecondsToString(startOffset));
        }

        if (format != null) {
            args.add("-f", format);
        }

        if (read_at_native_frame_rate) {
            args.add("-re");
        }

        args.addAll(extra_args);

        for (String input : inputs) {
            args.add("-i").add(input);
        }

        return args.build();
    }
}
