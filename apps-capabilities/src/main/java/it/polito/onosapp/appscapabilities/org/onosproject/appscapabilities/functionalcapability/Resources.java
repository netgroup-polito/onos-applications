package it.polito.onosapp.appscapabilities.org.onosproject.appscapabilities.functionalcapability;

/**
 * Created by gabriele on 09/10/16.
 */
public class Resources {

    private Cpu cpu;
    private Memory memory;
    private Storage storage;

    public Resources(Cpu cpu, Memory memory, Storage storage) {
        this.cpu = cpu;
        this.memory = memory;
        this.storage = storage;
    }

    public Cpu getCpu() {
        return cpu;
    }

    public Memory getMemory() {
        return memory;
    }

    public Storage getStorage() {
        return storage;
    }

    public static final class Cpu {
        private short number;
        private short frequency;
        private short free;

        public Cpu(short number, short frequency, short free) {
            this.number = number;
            this.frequency = frequency;
            this.free = free;
        }

        public short getNumber() {
            return number;
        }

        public short getFrequency() {
            return frequency;
        }

        public short getFree() {
            return free;
        }
    }

    public static final class Memory {
        private short size;
        private short frequency;
        private short latency;
        private short free;

        public Memory(short size, short frequency, short latency, short free) {
            this.size = size;
            this.frequency = frequency;
            this.latency = latency;
            this.free = free;
        }

        public short getSize() {
            return size;
        }

        public short getFrequency() {
            return frequency;
        }

        public short getLatency() {
            return latency;
        }

        public short getFree() {
            return free;
        }
    }

    public static final class Storage {
        private short size;
        private short free;
        private short latency;

        public Storage(short size, short free, short latency) {
            this.size = size;
            this.free = free;
            this.latency = latency;
        }

        public short getSize() {
            return size;
        }

        public short getFree() {
            return free;
        }

        public short getLatency() {
            return latency;
        }
    }

}
