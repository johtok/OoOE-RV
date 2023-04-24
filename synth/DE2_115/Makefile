
PROJECT = DE2_115
TOP_LEVEL = DE2_115
FAMILY = "Cyclone IV E"
PART = EP4CE115F29C7
CLOCK_PERIOD = 20
PINOUT = pins.tcl

PROJECT_FILES = $(PROJECT).qpf $(PROJECT).qsf $(PROJECT).sdc

SRCS = $(wildcard ../../build/*.v)


default: synth

clean:
	rm -rf *.sld *.rpt *.chg *.htm *.eqn *.pin *.sof *.pof db incremental_db *.summary *.smsg *.jdi *.done *.qws $(PROJECT_FILES)


setup: $(PROJECT_FILES)
synth: $(PROJECT).sof


$(PROJECT_FILES):
	quartus_sh --64bit --prepare -f $(FAMILY) -t $(TOP_LEVEL) -d $(PART) $(PROJECT)
	echo -e "\nset_global_assignment -name SOURCE_TCL_SCRIPT_FILE $(PINOUT)" >> $(PROJECT).qsf
	echo -e "create_clock -period $(CLOCK_PERIOD)  [get_ports {clock}]\nderive_clock_uncertainty" > $(PROJECT).sdc
	echo -e "set_global_assignment -name NUM_PARALLEL_PROCESSORS 8" >> $(PROJECT).qsf
	


$(PROJECT).sof: $(SRCS) $(PROJECT_FILES)
	quartus_map --64bit --read_settings_files=on $(addprefix --source=,$(SRCS)) $(PROJECT)
	quartus_fit --64bit --part=$(PART) --read_settings_files=on $(PROJECT)
	quartus_asm --64bit $(PROJECT)
	quartus_sta --64bit $(PROJECT)

program: $(PROJECT).sof
	killall jtagd || true
	jtagd
	jtagconfig
	quartus_pgm --no_banner --mode=jtag -o "P;$(PROJECT).sof"