# RV-OoOE processor
This is the README for the RISC-V out of order execution (OoOE) processor supporting the RISCVI ISA. 

## CONTENTS OF THIS FILE
This file includes:
- REQUIREMENTS - a list of requirements for being able to reproduce the verilog files necessary for synthesizing this project
- CHISEL GENEREATED VERILOG CODE - a turtorial to generate verilog code from the chisel files provided in this zip
- CONTENTS - the contents of this provided zip

## REQUIREMENTS
To be able to compile the RICVI OoOE processor openjdk version 8 or 11 has to be installed. We used openjdk version 11 from adoptium which can be downloaded from https://adoptopenjdk.net/releases.html.
 
## CHISEL GENEREATED VERILOG CODE
To generate the chisel code input "sbt run" in the terminal while in the root of the folder.

## Contents
The following is the output of the tree command run in a folder in which the provided ZIP file is unzipped:

$ tree
.
└── OoOE-RV
    ├── build.sbt
    ├── README.md
    └── src
        ├── main
        │   └── scala
        │       └── ooo
        │           ├── boards
        │           │   └── DE2_115.scala
        │           ├── Core.scala
        │           ├── modules
        │           │   ├── Arch2PhysMap.scala
        │           │   ├── DataMem.scala
        │           │   ├── Decoder.scala
        │           │   ├── EventArbiter.scala
        │           │   ├── Execute.scala
        │           │   ├── Execution
        │           │   │   ├── ALU.scala
        │           │   │   ├── Execution.scala
        │           │   │   └── InstructionDecoder.scala
        │           │   ├── IdAllocator.scala
        │           │   ├── InstructionStreamer.scala
        │           │   ├── IssueQueue.scala
        │           │   ├── MapSelector.scala
        │           │   ├── MemQueue.scala
        │           │   ├── OperandFetch.scala
        │           │   ├── ReorderBuffer.scala
        │           │   ├── Retirement.scala
        │           │   ├── SpeculativeArch2PhysMap.scala
        │           │   └── StateArch2PhysMap.scala
        │           ├── package.scala
        │           ├── Types.scala
        │           └── util
        │               ├── package.scala
        │               ├── Program.scala
        │               └── TriStateDriver.scala
        └── test
            ├── programs
            │   ├── addlarge.res
            │   ├── addlarge.s
            │   ├── addneg.res
            │   ├── addneg.s
            │   ├── addpos.res
            │   ├── addpos.s
            │   ├── bool.res
            │   ├── bool.s
            │   ├── branchcnt.res
            │   ├── branchcnt.s
            │   ├── branchmany.res
            │   ├── branchmany.s
            │   ├── branchtrap.res
            │   ├── branchtrap.s
            │   ├── call.res
            │   ├── call.s
            │   ├── linker.ld
            │   ├── loop.c
            │   ├── loop.res
            │   ├── Makefile
            │   ├── README.md
            │   ├── recursive.c
            │   ├── recursive.res
            │   ├── set.res
            │   ├── set.s
            │   ├── shift2.res
            │   ├── shift2.s
            │   ├── shift.res
            │   ├── shift.s
            │   ├── string.res
            │   ├── string.s
            │   ├── t10.res
            │   ├── t10.s
            │   ├── t11.res
            │   ├── t11.s
            │   ├── t12.c
            │   ├── t12.res
            │   ├── t13.c
            │   ├── t13.res
            │   ├── t14.res
            │   ├── t14.s
            │   ├── t15.res
            │   ├── t15.s
            │   ├── t1.res
            │   ├── t1.s
            │   ├── t2.res
            │   ├── t2.s
            │   ├── t3.res
            │   ├── t3.s
            │   ├── t4.res
            │   ├── t4.s
            │   ├── t5.res
            │   ├── t5.s
            │   ├── t6.res
            │   ├── t6.s
            │   ├── t7.res
            │   ├── t7.s
            │   ├── t8.res
            │   ├── t8.s
            │   ├── t9.res
            │   ├── t9.s
            │   ├── width.res
            │   └── width.s
            └── scala
                └── ooo
                    ├── CoreTest.scala
                    ├── modules
                    │   ├── IdAllocatorTest.scala
                    │   ├── IssueQueueTest.scala
                    │   └── StateArch2PhysMapTest.scala
                    └── util
                        └── TestingUtils.scala

16 directories, 95 files
