This repository contains the result of a [compiler course](https://pp.info.uni-karlsruhe.de/lehre/WS202122/compprakt/)
that we took during the winter term 2021/22 at [Karlsruher Institut für Technologie](https://www.kit.edu/). The task was
to implement a fully functional compiler for a subset of the Java language. This included (besides other things)
lexing the input, constructing an abstract syntax tree, transforming the programm
into [SSA form](https://en.wikipedia.org/wiki/Static_single_assignment_form) and running optimizations, before
outputting x86 assembly.

Most of the intermediate steps were to be done by using the research group's own compiler
library [libFirm](https://pp.ipd.kit.edu/firm/). Firm offers fully graph-based code representation and comes with a C99
frontend, enabling it to output optimized code for both x86 and SPARC architectures. While we were free to use whatever
part of the library we wanted during development, the final submission was only allowed to use basic primitives of
libfirm to interact with the graph. This meant that both optimizations and code generation had to done by our own
compiler. So calling our compiler `gentle` was the only logical thing do to.

The task were do be done in groups of four to five students, competing to compile the fastest *correct* binaries (
without taking forever to do so). We choose to implement our compiler in Java, simply for the fact, that we all had a
lot of experience using it and [bindings were already available](https://pp.ipd.kit.edu/git/jFirm/).

All of this was done by [@Chrisliebaer](https://github.com/chrisliebaer), [@Garmelon](https://github.com/Garmelon),
[@I-Al-Istannen](https://github.com/I-Al-Istannen), [@pbrinkmeier](https://github.com/pbrinkmeier)
and [@SirYWell](https://github.com/SirYwell).

# How to compile and run it
This project is using gradle for compilation. So if you are familiar with gradle, you can simply build the project as
you are used to. You can also use the `build` and `run` scripts, which where used by both the benchmark and testing suite.
We also build a Docker image which you can find on ghcr.io or build yourself by using the provided `Dockerfile`.

# Features
Gentle is a fully functional Mini-Java-Compiler, which is a limited subset of Java without most object-oriented
features. It contains all required compilation stages as well as the following optimizations:

* Algebraic Identity Transformation
* Boolean Optimizations
* Escape Analysis
* Global Value Numbering
* Constant folding (this one was required)
* Control Flow Optimizations
* Loop-invariant code motion
* Method Inlining
* Pure Function Analysis
* Strength Reduction
* Unused Parameter Elimination

Sadly we were unable to produce a proper register allocator which meant that the final submission was always loading
and storing the operands from stack. We were not required to have any form of register allocation but it would have
been nice, since the produced binary is actually really slow and would have benefited greatly from having a solid
register allocation.

# Summary
There was an optional competition for generating the fastest binaries, that we probably had very good chances of winning,
given the capabilities of our code transformations during optimization. If it weren't for having not register allocation at all
We ended up third place, which is right in the middle and was probably due to gentle being very robust, winning simply
by default, for generating working binaries, where other compilers failed.

Despite that, we had lots of fun working on this project and really learned a lot about compilers. We had to invest
quite a lot of time, probably than you would in other courses, but who cares if you enjoy it? It's probably not advised
to take this course if you are not interested and just want to get easy credits.

As this repository demonstrates, we also went ham with the CI pipeline, creating multiple tests and unnecessary tools to
save a few hours in total, that we never got back. On of which is [Flammenwehrfer](https://github.com/Flammenwehrfer),
our cross-repository test bot, that would run all tests from our shared test framework against every groups current
compiler, informing them if a test case broken their compiler.

# FAQ
The following section serves to answer what we believe to be common questions one might have, especially if you are a
fellow student and consider taking this course.

## What Language does it compile? Is it a real compiler?
gentle is able to compile *Mini-Java*. Mini-Java is a subset of Java (technically it contains programs which would be
invalid in Java due to some edge cases). The language has been purposfully modified to reduce the scope of supported
language features while maintaining all essential control primitives. You might have realized that this results in a
very clunky to use language and you are correct. However the goal of this course was not in fact to engineer a good
language but focus on the optimizations and code generation. Here is a quick summary of most of Mini-Java's key aspects.

* mostly subset of Java
* no dynamic binding / interfaces / vtables
* only one input file
* **very** basic IO (only able to read and write to/from `stdin/stdout`)
* no memory managment, memory will never be `free`-ed
* no constructors / no overloading / no visibility modifiers (or any modifiers)
* no static variables, `public static void main(String[])` is only static method (and kept to stay mostly compatible
  with Java)

## But isn't Java using garbage collection and supposed to run everywere?
The Java language itself only tells us the semantics of the language. It does in fact not specify the requirement of a
garbage collector. That's the job of the JVM specification. There is nothing preventing you from compiling it directly
to native code.

## This sounds really complicated, how did you not get overwhelmed?
For starters, don't make mistakes, duh. (If you do, blame someone else, the last person to have merged code
into `master` is a good candidate to start with. If it was you, blame the person who wrote the failing test.)

Part of the assignments was to contribute test cases to a common pool of tests, shared by all competing groups. This
way we had solid foundation of test cases, that we could all use to test our compiler against. However as we later
learned, our collection of tests turned out to be very lackluster, failing to spot quite a lot of very basic cases,
which meant that we had to rely quite often on our mentors, who would have their own test suite from all the years before
us combined. Towards the end of the project we started using [jazzer](https://github.com/CodeIntelligenceTesting/jazzer)
which was kind of eye-opening, as it was able to spit out bugs faster than any test before that ever could, despite
being hand-crafted.
