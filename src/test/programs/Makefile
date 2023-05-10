

ASRCS := $(wildcard *.s)
CSRCS := $(wildcard *.c)
AOBJ := $(ASRCS:%.s=%.out)
COBJ := $(CSRCS:%.c=%.out)
BINS := $(COBJ:%.out=%.bin) $(AOBJ:%.out=%.bin)
RES := $(BINS:%.bin=%.res)

SIMULATOR ?= ~/onedrive/devel/simson/target/release/simson

GCC := riscv64-linux-gnu

all: ${AOBJ} ${COBJ} ${BINS} ${RES}

obj: ${AOBJ} ${COBJ}

bin: ${BINS}

res: ${RES}

%.out: %.c
	$(GCC)-gcc -ffreestanding -nostartfiles -nostdlib -nodefaultlibs -Wl,-T,linker.ld $< -o $@ -march=rv32i -mabi=ilp32
	$(GCC)-objdump $@ -d

%.out: %.s
	$(GCC)-as $< -o $@ -march=rv32i -mabi=ilp32
	$(GCC)-objdump $@ -d

%.bin: %.out
	$(GCC)-objcopy -O binary $< $@

%.res: %.bin
	$(SIMULATOR) -d $@ $<

clean:
	@rm *.bin *.out
