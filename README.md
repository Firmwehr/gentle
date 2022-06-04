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

# Summary
To spice up the course a bit a competition was held and a prize awarded for the
compiler producing the fastest (and correct of course!) binaries.
Gentle has quite a few interesting optimizations but some major parts, like
load-store optimizations and especially register allocation, weren't finished in
time.

Despite this we had lots of fun working on the project and learned a lot about
how compilers work and why they do(n't do) certain things.
Learning this on our own time and happily cooking up weird optimizations beyond
what was needed took up considerable time though and far exceeded what the
course credits suggest.
If you are interested in compilers and have fun hacking away on one, the course
is still a good choice :)

As our team consisted of quite a few people with an interest in modern software
development practices, we also went ham with our CI pipeline and static
analysis tools, resulting in a quite robust and stable compiler.
During the course we integrated Qodana, wrote a Github Action to interpret its
output, created a docker image, standardized on a development workflow with
mandatory code review and also wrote
[Flammenwehrfer](https://github.com/Flammenwehrfer) while we were at it.
Flammenwehrfer is our cross-repository test bot, runing all tests from our
shared test framework against the compilers of every group, opening and closing
github issues depending on how well their compiler coped with new tests.
Not yet satisfied with the existential dread we caused, we continued on and
wrote an annotation processor parsing Ascii-Art program graphs and emitting
type-safe tree matching code we could use from java.

# FAQ
The following section serves to answer what we believe to be common questions,
especially if you are a fellow student and consider taking this course.

## What language does it compile? Is it a real compiler?
gentle is able to compile *Mini-Java*. Mini-Java is a subset of Java (technically it contains programs which would be
invalid in Java due to some edge cases).
The language has been purposfully modified to reduce the scope of supported
language features while maintaining all essential control primitives.
It completly elides polymorphism, for a start, but also simpler things like for loops:
After all, a `while` statement is perfectly sufficient!
You might suspect that writing programs in MiniJava would be extremely tedious as a result and no sane person would try to write code in it.
This isn't wrong, but as you might have notices by now we are *far* from sane.
During the course we wrote our own from-scratch version of the popular
asciiquarium, a brainfuck interpreter, a fraction-based raytracer, a solution
for the first day of Advent Of Code and a few more larger programs.

To better understand what we are dealing with, you can find a summary of Mini-Java below:

* (mostly) a subset of Java
* no dynamic binding / interfaces / vtables
* only one input file
* **very** basic IO (only able to read and write to/from `stdin/stdout`)
* no memory managment, memory will never be `free`-ed
* no constructors / no overloading / no visibility modifiers (or any modifiers)
* no static variables and methods

## But isn't Java using garbage collection and supposed to run everywere?
The Java language itself only defines the *semantics* of the language, not implementation details.
It does, in fact, *not* require a garbage collector.
There is nothing preventing you from compiling it directly to native code and
there exist projects that do just that (e.g. Oracle's `graal` native-image).

## This sounds really complicated, how did you not get overwhelmed?
Quite simple: Don't make mistakes, duh.
(If you do, blame someone else! The last person to have merged code into
`master` is a good candidate to start with. If it was you or you reviewed it,
blame the person who wrote the failing test. Resort to
[git-blame-someone-else](https://github.com/jayphelps/git-blame-someone-else)
if necessary.)

Part of the assignment was to contribute test cases to a common pool, shared by all groups.
This ensured a solid foundation of test cases existed.
However, as we later learned, our collection of tests turned out to be very lackluster.
It failed to spot quite a lot of very basic bugs, forcing us to rely on our mentors to report bugs in our weekly meetings.
They could fall back on the combined test suite of all the years before us,
allowing them to uncover bugs a lot more efficiently.

Towards the end of the project we started using
[jazzer](https://github.com/CodeIntelligenceTesting/jazzer) which was
eye-opening: It found a bug we were stumped on for a week in a few minutes and
continued to produce bugs crashing our compiler, the compiler of other groups
and even the holy reference compiler itself.
