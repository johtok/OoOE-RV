
SRCS = $(wildcard src/scala/**/*.scala)
TARGETS = DE2_115

$(TARGETS): % : build/%.v
	$(MAKE) -C synth/$@ synth

build/%.v: $(SRCS)
	sbt "runMain ooo.boards.$(notdir $(basename $@))"

clean:
	rm *.v *.fir *.anno.json firrtl_black_box_resource_files.f || true
	rm build/* || true
	rmdir build || true