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

All of this was done by @Chrisliebaer, @Garmelon, @I-Al-Istannen, @pbrinkmeier and @SirYWell

# How to compile and run it
TODO: docker image, gradlew

# Features
TODO: features, optimization stages

# Feedback
TODO: feedback

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
* TODO

## But isn't Java using garbage collection and supposed to run everywere?
The Java language itself only tells us the semantics of the language. It does in fact not specify the requirement of a
garbage collector. That's the job of the JVM specification. There is nothing preventing you from compiling it directly
to native code. (TODO: fact check, elaborate)

## This sounds really complicated, how did you not get overwhelmed?
For starters, don't make mistakes, duh. (If you do, blame someone else, the last person to have merged code
into `master` is a good candidate to start with. If it was you, blame the person who wrote the failing test.)

Part of the assignments was to contribute test cases shared by all competing groups. (TODO: continue tomorrow)
