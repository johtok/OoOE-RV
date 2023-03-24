
SRCS = $(wildcard src/scala/**/*.scala)
TARGETS = DE2_115

$(TARGETS): % : build/%.v
	$(MAKE) -C synth/$@ synth

build/%.v: $(SRCS)
	sbt "runMain ooo.boards.$(notdir $(basename $@))"

clean:
	rm build/* || true
	rmdir build || true