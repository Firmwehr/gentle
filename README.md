<div align="center">
  <h1>Gentle</h1>
</div>

Gentle is a compiler for a Java subset called "MiniJava" and was developed during the
2021/22 [compiler construction course](https://pp.info.uni-karlsruhe.de/lehre/WS202122/compprakt/)
at [Karlsruher Institut für Technologie](https://www.kit.edu/).
In this course, students implement a fully-functional compiler from the ground
up. Gentle is comprised of
- a simple string-based lexer
- a recursive-descent parser utilizing precedence climbing for expressions
  and building an abstract syntax tree
- a semantic analysis and conversion step, attributing our AST, enriching it
  with e.g. type information and ensuring programs are semantically valid
- a conversion into the [libFirm](https://pp.ipd.kit.edu/firm/) SSA-based
  intermediate representation (IR)
- many optimizations operating on the libFirm IR
- a backend step lowering the libFirm IR to our own Ikea-IR
- a final step generating x86-64 assembly
- invoking gcc to assemble and link the output with our custom runtime


## Intermediate Representations
As mentioned above, our compiler makes heavy use of the FIRM intermediate
representation using [libFirm](https://libfirm.github.io/). Firm is a
graph-based intermediate representation in
[SSA form](https://en.wikipedia.org/wiki/Static_single_assignment_form) and
libFirm offers a C99 frontend (`cparser`) and can output optimized assembly for
multiple architectures, including x86 (32/64 bit), RISC-V 32, MIPS and a few
more.

We weren't allowed to use many of the conveniences and analysis results libFirm
computes, so our compiler only depends on the graph structure, the initial
insertion of Phis to build up a proper SSA structure and dominance to properly
implement global value numbering.


## Integrating libFirm
libFirm is written in C and must therefore be called using JNI - the Java
native interface. Thankfully, there is already a library providing JNA (Java
native access) bindings for it: [jFirm](https://pp.ipd.kit.edu/git/jFirm/). We
forked this project, converted it a more standard maven build and included a
few pre-built native binaries. This ensures that gentle is self-contained and
can be run on its own, without needing to also manually compile and provide a
libFirm binary. You can find this fork
[in our organization](https://github.com/Firmwehr/jFirm).


## Organization and authors
As you can't effectively build a compiler with 20 people working all over each
other, we were split in groups of four to five students. Each group created
their own compiler for the same MiniJava language, but the approaches,
workflows and even programming languages varied considerably.

Our group consisted of [@Chrisliebaer](https://github.com/chrisliebaer),
[@Garmelon](https://github.com/Garmelon),
[@I-Al-Istannen](https://github.com/I-Al-Istannen),
[@pbrinkmeier](https://github.com/pbrinkmeier) and
[@SirYWell](https://github.com/SirYwell).


# Getting started
The easiest way to get started is using the `build` and `run` scripts in the
root directory. These scripts are used by CI and also the benchmark and testing
suite - they should work out of the box.

Additionally, we also provide a docker image built with
[jib](https://github.com/GoogleContainerTools/jib) which is automatically
pushed to the github container registry. You can find it
[here](https://github.com/Firmwehr/gentle/pkgs/container/gentle).

Finally, if you are familiar with gradle, you can also directly build gentle using it.
The `build` script is just a thin wrapper around `distInstall`.

# Features
Gentle supports every feature MiniJava has to offer, but you will have to live
without some conveniences. Notably, MiniJava does not contain any
object-oriented features like inheritance or some basic concepts like
`for`-loops or a `String` type. This doesn't stop you from writing proper
programs in it though! Our team produced a working from-scratch
re-implementation of the classic AsciiQuarium as well as a fully-functional
fraction-based raytracer. Some dedicated individuals even re-implemented
bitwise operations using addition, subtraction, multiplication and division
resulting in a few beautiful images of Voronoi-noise.

## Optimizations
Gentle has quite a few optimizations, many of which do indeed rapidly speed up
the produced binaries. The main choke point currently is the lack of a proper
register allocator. The "Lego" backend contains a WIP graph coloring register
allocator, but that didn't get finished in time. It can produce valid coloring
for simple programs, but has problems with anything else. Turns out, graph
based register allocation isn't easy :) The implemented optimizations are quite
decent and it would have been interesting to see how fast the compiler would be
with a proper backend.

Currently gentle implements the following optimizations:
* **Algebraic identities** (distributive, associative, additive/multiplicative
  identities and many more)
* **Boolean optimizations** (e.g. elimination of unnecessary jumps)
* **Escape analysis** (able to completely omit allocating objects or statically
  accessed arrays)
* **Global Value Numbering** (a form of common subexpression elimination)
* **Constant folding (this one was required)** (computes constant expressions
  at compile-time)
* **Control Flow Optimizations** (e.g. omit unnecessary jumps, merge blocks
  where possible, …)
* **Loop-invariant code motion** (move computations outside of loops if they
  don't depend on the iteration)
* **Method Inlining** (essentially copy-paste one method into all callsites)
* **Pure Function Analysis** (e.g. deduplicate calls to pure functions or omit
  calling them if the result is never used)
* **Strength Reduction** (e.g. convert costly divisions and multiplications
  into bit shifts)
* **Unused Parameter Elimination** (remove parameters from methods if they are
  never used)

Current rewrite border
----

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
