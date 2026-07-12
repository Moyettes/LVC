package com.moyettes.legacyvoicechat.audio;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Queue;

public class AdaptiveJitterBuffer {

    private final long packetDelayMillis;
    private final Queue<BufferedPacket> queue;

    private long firstPacketArrival = -1;
    private long firstSequenceNumber = -1;
    private long lastPacketArrival = -1;
    private double jitterEstimate = 0.0;
    private long adaptiveDelayMillis;

    public AdaptiveJitterBuffer(int packetDelay) {
        this.packetDelayMillis = packetDelay * 20L;
        this.adaptiveDelayMillis = packetDelayMillis;

        if (packetDelay <= 1) {
            this.queue = new LinkedBlockingQueue<>();
        } else {
            this.queue = new PriorityBlockingQueue<>(
                packetDelay * 2,
                (a, b) -> Long.compare(a.sequenceNumber, b.sequenceNumber)
            );
        }
    }

    public void offer(byte[] audioData, Integer senderId, long sequenceNumber,
                     double distance, float volume, double playerX, double playerY, double playerZ) {

        long arrivalTime = System.currentTimeMillis();

        if (lastPacketArrival != -1) {
            long transit = arrivalTime - lastPacketArrival;
            long delta = Math.abs(transit - 20);
            jitterEstimate += (delta - jitterEstimate) / 16.0;
            adaptiveDelayMillis = Math.round(jitterEstimate / 20.0) * 20;
        }
        lastPacketArrival = arrivalTime;

        if (firstSequenceNumber == -1) {
            firstPacketArrival = arrivalTime;
            firstSequenceNumber = sequenceNumber;
        }

        BufferedPacket packet = new BufferedPacket(
            audioData, sequenceNumber, distance, volume,
            playerX, playerY, playerZ, arrivalTime,
            calculateScheduledPlaybackTime(sequenceNumber, arrivalTime)
        );

        queue.offer(packet);
    }

    private long calculateScheduledPlaybackTime(long sequenceNumber, long arrivalTime) {
        if (firstSequenceNumber == -1) {
            return arrivalTime + packetDelayMillis;
        }

        long sequenceOffset = sequenceNumber - firstSequenceNumber;
        return firstPacketArrival + packetDelayMillis + (sequenceOffset * 20);
    }

    public BufferedPacket poll() {
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void reset() {
        queue.clear();
        firstSequenceNumber = -1;
        firstPacketArrival = -1;
        lastPacketArrival = -1;
        jitterEstimate = 0.0;
        adaptiveDelayMillis = packetDelayMillis;
    }

    public static class BufferedPacket {
        public final byte[] audioData;
        public final long sequenceNumber;
        public final double distance;
        public final float volume;
        public final double playerX;
        public final double playerY;
        public final double playerZ;
        public final long arrivalTime;
        public final long scheduledPlaybackTime;

        public BufferedPacket(byte[] audioData, long sequenceNumber, double distance,
                            float volume, double playerX, double playerY, double playerZ,
                            long arrivalTime, long scheduledPlaybackTime) {
            this.audioData = audioData;
            this.sequenceNumber = sequenceNumber;
            this.distance = distance;
            this.volume = volume;
            this.playerX = playerX;
            this.playerY = playerY;
            this.playerZ = playerZ;
            this.arrivalTime = arrivalTime;
            this.scheduledPlaybackTime = scheduledPlaybackTime;
        }
    }
}
