# How languages praised for their error messages actually do it — and what the research says

Research notes, 2026-09-06. Every claim below is tied to a URL that was actually fetched during this session; quotes are verbatim from the fetched text unless marked "(summarised)". Raw copies of everything fetched are in `research/raw/` next to this file (Elm compiler `.hs` files, Ruby gem sources, changelogs, PDF text extractions, etc.).

Conventions: **CT** = compile-time diagnostic, **RT** = runtime error/traceback.

---

## 1. Elm — the whole budget is spent at compile time

### 1.1 The two design posts (Evan Czaplicki, 2015)

The pages at elm-lang.org are served as a compiled Elm bundle (the article text is embedded in JavaScript), so the quotes below are taken from the site's source repository, which is what the pages render:

- "Compiler Errors for Humans" (Elm 0.15.1, dated 2015-06-30): https://elm-lang.org/news/compiler-errors-for-humans — source: https://github.com/elm/elm-lang.org/blob/master/pages/news/compiler-errors-for-humans.elm
- "Compilers as Assistants" (Elm 0.16, dated 2015-11-19): https://elm-lang.org/news/compilers-as-assistants — source: https://github.com/elm/elm-lang.org/blob/master/pages/news/compilers-as-assistants.elm

**Stated principles, "Compiler Errors for Humans":**

- The thesis: "you can make a shockingly huge difference just by thinking about the user experience".
- Locating the code first: "Before you can resolve an error, you need to find the code causing it." The post lists the two costs of the traditional `program.x:43:22` format: "converting row and column numbers into an actual file position" and "converting pretty-printed code onto actual code to verify that position". The fix: "**The error shows the code exactly as you wrote it.** ... The error message shows the exact code you wrote *and* the corresponding line numbers."
- Explaining in the user's terms: "Saying “these two types do not match” is exactly went wrong for the compiler, but how the hell does that relate to my code?! Again, you are doing a mental translation from “the compiler is angry” to something actually useful. Why not try to do that automatically?" → "**Every message has a useful hint.**"
- Cost of doing this: "**Technical Note:** I found that generating such specific error messages required no significant changes to the type inference algorithm and imposed no noticeable performance cost. I just added an extra bit of info to each type constraint."
- Visual layout: "We use color in two ways here. The red draws your attention to the problem. ... The blue serves as a visual separator between error messages." and "We use layout to reveal detail as needed. General context is *above* the code. It is very short and hints at what is wrong in the code snippet. ... If not, more specific hints are *below* the code."
- Machine-readable form from day one: "When you add the `--report=json` flag, our build tool can now spit out JSON error messages that are easy to read in to any editor plugin out there."
- Feedback loop: "I set up the error-message-catalog to keep improving Elm. It is a collection of Elm programs that trigger error messages." (https://github.com/evancz/error-message-catalog, as linked from the post)

**Stated principles, "Compilers as Assistants":**

- "**Compilers should be assistants, not adversaries.** A compiler should not just *detect* bugs, it should then help you understand *why* there is a bug. It should not berate you in a robot voice, it should give you specific hints that help you write better code."
- The improvements were user-driven: "Users’ reports of confusing error messages actually motivated most of the improvements in this release."
- Type diffs: "The Elm compiler now does **type diffs** where it compares any two types and highlights the differences. Notice that it hides information that is not directly relevant ... It even does typo detection for record fields".
- Expected vs actual, but not dogmatically: "this expected vs actual dichotomy does not *really* make sense in a lot of cases. ... when the branches of an `if` do not match. Which is the expected one? ... the compiler just “does the right thing” for each scenario."
- Beginner hints for predictable mistakes (using `+` on strings, "truthy" values).
- "**No More Cascading Errors** ... a single mistake can lead to 3 or 4 different error messages, leaving the programmer to figure out which one is the *real* problem. Well, there are no more cascading errors in Elm 0.16".

### 1.2 Why Elm has essentially no runtime error messages to design

Same post, section "Catching More Bugs": "One big goal of Elm is to have no runtime errors. In practice, this is pretty much already how it goes. You can go months or years without a runtime error. In any case, Elm 0.16 closes one of the last remaining loopholes." (incomplete pattern matches become compile errors).

The home page (https://elm-lang.org/, source https://github.com/elm/elm-lang.org/blob/master/pages/home.elm) rotates the tagline "with no runtime exceptions." and has a section titled "No Runtime Exceptions": "Elm uses type inference to detect corner cases and give friendly hints. NoRedInk switched to Elm about four years ago, and 300k+ lines later, they still have not had to scramble to fix a confusing runtime exception in production." The example it shows is a compile-time message:

```
-- TYPE MISMATCH ---------------------------- Main.elm

The 1st argument to `drop` is not what I expect:

8|   List.drop (String.toInt userInput) [1,2,3,4,5,6]
                ^^^^^^^^^^^^^^^^^^^^^^
This `toInt` call produces:

    Maybe Int

But `drop` needs the 1st argument to be:

    Int

Hint: Use Maybe.withDefault to handle possible errors.
```

Consequence for architecture: because there are (by design) no exceptions to report at runtime, all of Elm's effort goes into the compiler, where it has full type information, the original source bytes, and exact source regions for every AST node. Nothing analogous to a "runtime error message" subsystem exists to study.

### 1.3 Data model in the compiler (github.com/elm/compiler, `master`)

- `compiler/src/Reporting/Report.hs` — the entire module is one record:
  ```haskell
  data Report = Report
    { _title   :: String
    , _region  :: A.Region
    , _sgstns  :: [String]
    , _message :: D.Doc
    }
  ```
  https://github.com/elm/compiler/blob/master/compiler/src/Reporting/Report.hs
- `compiler/src/Reporting/Annotation.hs` — every AST node is `data Located a = At Region a`; `Region` is a start/end `Position` (row+column packed into a `Word64`), with `toEditorRowCol` converting to 1-based editor coordinates. https://github.com/elm/compiler/blob/master/compiler/src/Reporting/Annotation.hs
- `compiler/src/Reporting/Error.hs` — one sum type per phase (`BadSyntax`, `BadImports`, `BadNames`, `BadTypes L.Localizer …`, `BadMains`, `BadPatterns`, `BadDocs`); `toReports :: Code.Source -> Error -> NE.List Report.Report` turns each into reports **given the source**; a `Module` record carries `_source :: B.ByteString` so the renderer always has the original text. Terminal rendering (`toDoc`) prints a cyan header bar `"-- " ++ title ++ " ---- " ++ path`, then the message. JSON rendering (`toJson`) emits `{"path", "name", "problems":[{"title", "region":{"start":{"line","column"},"end":{…}}, "message": …}]}`. https://github.com/elm/compiler/blob/master/compiler/src/Reporting/Error.hs
- `compiler/src/Reporting/Doc.hs` — a thin layer over `Text.PrettyPrint.ANSI.Leijen`: `toAnsi` (colour, 80 cols) vs `toString` (plain); `reflow` for prose; `toSimpleHint`/`toFancyHint` render an underlined `Hint:` prefix, `toSimpleNote`/`toFancyNote` a `Note:`; `link`/`makeLink` build `<https://elm-lang.org/<compiler-version>/<file>>` pointers to longer docs; `encode :: P.Doc -> E.Value` serialises a rendered doc as a JSON array of plain strings and `{"bold","underline","color","string"}` chunks, so editors get the same styled text the terminal shows. https://github.com/elm/compiler/blob/master/compiler/src/Reporting/Doc.hs
- `compiler/src/Reporting/Render/Code.hs` — `newtype Source = Source [(Word32, String)]` (numbered lines); `toSnippet source region maybeSubRegion (preHint, postHint)` = prose above, the source lines in `region` with `N|` gutters, a red `>` marker on affected lines, a red `^^^^` underline under the sub-region when it is on a single line (`makeUnderline`), and prose below; `toPair` renders two regions (e.g. both branches of an `if`). https://github.com/elm/compiler/blob/master/compiler/src/Reporting/Render/Code.hs
- `--report=json`: `terminal/src/Make.hs` defines `data ReportType = Json` and a `--report` flag whose parser only accepts `"json"`; `builder/src/Reporting/Exit/Help.hs` emits either `{"type":"compile-errors","errors":[…Error.toJson…]}` or `{"type":"error","path","title","message"}`, and its terminal path only uses ANSI colour if `hIsTerminalDevice`. https://github.com/elm/compiler/blob/master/terminal/src/Make.hs , https://github.com/elm/compiler/blob/master/builder/src/Reporting/Exit/Help.hs

Takeaway: title + region + prose-doc, rendered against the retained source, with one JSON encoding shared by all tools. The `_sgstns :: [String]` field (suggestions) exists in the data model even though the terminal renderer in `Error.hs` ignores it (`Report.Report title _ _ message`).

---

## 2. Rust — rich structured compile-time diagnostics, deliberately terse runtime panics

### 2.1 The rustc diagnostics architecture (rustc-dev-guide)

Source: https://rustc-dev-guide.rust-lang.org/diagnostics.html (fetched; quotes verbatim).

- **Structure.** A diagnostic has a level, an optional code, a message ("the main description of the problem. It should be general and able to stand on its own"), a "diagnostic window" (file path, line/col, code snippet, spans with labels) and sub-diagnostics. In source (https://github.com/rust-lang/rust/blob/master/compiler/rustc_errors/src/diagnostic.rs):
  ```rust
  pub struct DiagInner {
      pub(crate) level: Level,
      pub messages: Vec<(DiagMessage, Style)>,
      pub code: Option<ErrCode>,
      pub lint_id: Option<LintExpectationId>,
      pub span: MultiSpan,
      pub children: Vec<Subdiag>,
      pub suggestions: Suggestions,
      pub args: DiagArgMap,
      pub sort_span: Span,
      pub is_lint: Option<IsLint>,
      ...
  }
  pub struct Subdiag { pub level: Level, pub messages: Vec<(DiagMessage, Style)>, pub span: MultiSpan }
  ```
- **Levels.** `error` ("emitted when the compiler detects a problem that makes it unable to compile the program"), `warning`, `help` ("emitted following an `error` or `warning` to give additional information to the user about how to solve their problem. These messages often include a suggestion string and `rustc_errors::Applicability` confidence level to guide automated source fixes by tools"), `note` ("emitted to give more context and identify additional circumstances and parts of the code that caused the warning or error"). Rule: "The error or warning portion should not suggest how to fix the problem, only the “help” sub-diagnostic should."
- **Span labels.** "Primary spans should have enough text to describe the problem in such a way that if it were the only thing being displayed (for example, in an IDE) it would still make sense."
- **Suggestions + Applicability.** "`Diag` offers a structured suggestions API, which formats code suggestions pleasingly in the terminal, or (when the `--error-format json` flag is passed) as JSON for consumption by tools like `rustfix`. Not all suggestions should be applied mechanically; they have a degree of confidence in the suggested code, from high (`Applicability::MachineApplicable`) to low (`Applicability::MaybeIncorrect`)." The four variants: `MachineApplicable` ("Can be applied mechanically"), `HasPlaceholders`, `MaybeIncorrect`, `Unspecified` ("Cannot be applied mechanically because we don’t know which of the above cases it falls into").
- **Suggestion wording (note the contrast with Python/Ruby/Elixir/Gleam).** "Suggestions should not be a question. In particular, language like “did you mean” should be avoided. Sometimes, it’s unclear why a particular suggestion is being made. In these cases, it’s better to be upfront about what the suggestion is. Compare “did you mean: `Foo`” vs. “there is a struct with a similar name: `Foo`”."
- **Message style.** "The text should be matter of fact and avoid capitalization and periods, unless multiple sentences are _needed_"; "When code or an identifier must appear in a message or label, it should be surrounded with backticks"; "Error messages should be succinct. Users will see these error messages many times, and more verbose descriptions can be viewed with the `--explain` flag. That said, don’t make it so terse that it’s hard to understand."; "The word “illegal” is illegal. Prefer “invalid” or a more specific word instead."
- **Error codes / `--explain`.** "Most errors have an associated error code. Error codes are linked to long-form explanations which contains an example of how to trigger the error and in-depth details about the error. They may be viewed with the `--explain` flag, or via the error index. As a general rule, give an error a code (with an associated explanation) if the explanation would give more information than the error itself."
- **Lints vs errors.** "Some messages are emitted via lints, where the user can control the level. Most diagnostics are hard-coded such that the user cannot control the level." Lint levels: `deny` ("Equivalent to `error` diagnostic level"), `warn`, `allow`.
- **JSON emitter.** `--error-format json`; "Note that the output is a series of lines, each of which is a JSON object, but the series of lines taken together is, unfortunately, not valid JSON." The object carries a `rendered` field with "the 'human' output as a string". Field-level spec at https://doc.rust-lang.org/rustc/json.html: top-level `message`, `code{code, explanation}`, `level`, `spans[]`, `children[]` ("children are not nested"), `rendered`; each span has `file_name`, `byte_start/byte_end`, `line_start/line_end`, `column_start/column_end`, `is_primary`, `text[]`, `label`, `suggested_replacement`, `suggestion_applicability`, `expansion` (macro provenance). "JSON messages are emitted one per line to stderr."
- **annotate-snippets emitter.** rustc ships a second human emitter built on the `annotate-snippets` crate: `compiler/rustc_errors/src/annotate_snippet_emitter_writer.rs` opens with "Emit diagnostics using the `annotate-snippets` library. This is the equivalent of `./emitter.rs` but making use of the `annotate-snippets` library instead of building the output ourselves." and defines `pub struct AnnotateSnippetEmitter`. https://github.com/rust-lang/rust/blob/master/compiler/rustc_errors/src/annotate_snippet_emitter_writer.rs (The dev-guide page itself does not mention annotate-snippets; the crate is the extracted/shared implementation of rustc's rendering style.)

### 2.2 RFC 1644 "Default and Expanded rustc Errors" (2016-06-07)

https://rust-lang.github.io/rfcs/1644-default-and-expanded-rustc-errors.html (raw markdown also fetched from rust-lang/rfcs).

- Summary: "This RFC proposes an update to error reporting in rustc. Its focus is to change the format of Rust error messages and improve --explain capabilities to focus on the user's code. The end goal is for errors and explain text to be more readable, more friendly to new users, while still helping Rust coders fix bugs as quickly as possible."
- Motivation: "Rust offers a unique value proposition in the landscape of languages in part by codifying concepts like ownership and borrowing. Because these concepts are unique to Rust, it's critical that the learning curve be as smooth as possible. And one of the most important tools for lowering the learning curve is providing excellent errors that serve to make the concepts less intimidating, and to help 'tell the story' about what those concepts mean in the context of the programmer's code."
- Goals of the new format (verbatim list): "Create something that's visually easy to parse"; "Remove noise/unnecessary information"; "Present information in a way that works well for new developers, post-onboarding, and experienced developers without special configuration"; "**Draw inspiration from Elm** as well as Dybuk and other systems that have already improved on the kind of errors that Rust has."
- Labels: "The primary label is the label associated with the main warning/error. It explains the **what** of the compiler message. ... This label is colored to match the level of the message (yellow for warning, red for error) and uses the ^^^ underline." / "Secondary labels help to understand the error and use blue text and --- underline. These labels explain the **why** of the compiler message." / "Taken together, primary and secondary labels create a 'flow' to the message."
- The format it introduced (from the RFC):
  ```
  error[E0499]: cannot borrow `foo.bar1` as mutable more than once at a time
    --> src/test/compile-fail/borrowck/borrowck-borrow-from-owned-ptr.rs:29:22
     |
  28 |      let bar1 = &mut foo.bar1;
     |                      -------- first mutable borrow occurs here
  29 |      let _bar2 = &mut foo.bar1;
     |                       ^^^^^^^^ second mutable borrow occurs here
  31 |  }
     |  - first borrow ends here
  ```

### 2.3 Runtime panics are deliberately terse

The Rust Book, ch. 9.1 (https://doc.rust-lang.org/book/ch09-01-unrecoverable-errors-with-panic.html) shows the complete default output of a panic:

```
thread 'main' panicked at src/main.rs:2:5:
crash and burn
note: run with `RUST_BACKTRACE=1` environment variable to display a backtrace
```

and for an index error: `index out of bounds: the len is 3 but the index is 99`. The backtrace is opt-in; the book's guidance for reading one: "The key to reading the backtrace is to start from the top and read until you see files you wrote. That's the spot where the problem originated." The `std::panic!` docs (https://doc.rust-lang.org/std/macro.panic.html) confirm the hook: "The behavior of the default `std` hook, i.e. the code that runs directly after the panic is invoked, is to print the message payload to `stderr` along with the file/line/column information of the `panic!()` call." So: one location, one message, no source excerpt, no frames unless asked — the opposite investment profile from the compiler.

### 2.4 The Rust diagnostics-rendering crate ecosystem (used by other languages' compilers)

Descriptions are the crates.io metadata as fetched via the crates.io API on 2026-09-06:

- **annotate-snippets** (rust-lang) — "Format diagnostic reports, including highlighting snippets of text". https://github.com/rust-lang/annotate-snippets-rs (rustc's own alternative emitter uses it, see 2.1).
- **codespan-reporting** — "Beautiful diagnostic reporting for text-based programming languages". https://github.com/brendanzab/codespan (used by Gleam, see §6).
- **ariadne** — "A fancy diagnostics & reporting crate". https://github.com/zesterer/ariadne
- **miette** — "Fancy diagnostic reporting library and protocol for us mere mortals who aren't compiler hackers." https://github.com/zkat/miette — its README also advertises "a screen-reader-oriented diagnostic printer that's enabled in various situations, such as when you use NO_COLOR or CLICOLOR settings, or on CI".

---

## 3. Python — a dynamic language that improved *runtime* tracebacks

### 3.1 PEP 657 "Include Fine Grained Error Locations in Tracebacks" (Python 3.11)

https://peps.python.org/pep-0657/ (also fetched as RST source from python/peps). Authors: Pablo Galindo Salgado, Batuhan Taskaya, Ammar Askar.

- Abstract: "This PEP proposes adding a mapping from each bytecode instruction to the start and end column offsets of the line that generated them as well as the end line number. This data will be used to improve tracebacks displayed by the CPython interpreter in order to improve the debugging experience."
- Motivation: "The primary motivation for this PEP is to improve the feedback presented about the location of errors to aid with debugging." and "Python currently keeps a mapping of bytecode to line numbers from compilation" but "a single line of Python code can compile into dozens of bytecode operations making it hard to track which part of the line caused the error."
- What the user sees (from the PEP):
  ```
  x['a']['b']['c']['d'] = 1
  ~~~~~~~~~~~^^^^^
  TypeError: 'NoneType' object is not subscriptable
  ```
  `~` marks the whole failing expression, `^` the sub-expression that actually raised.
- How it works: "The offset information will be exposed to Python via a new attribute in the code object class called `co_positions` that will return a sequence of four-element tuples containing the full location of every instruction (including start line, end line, start column offset and end column offset) or `None` if the code object was created without the offset information." Plus a C API `int PyCode_Addr2Location(PyCodeObject *co, int addrq, int *start_line, int *start_column, int *end_line, int *end_column)`. "The current implementation, however, utilizes `uint8_t` types as an implementation detail to minimize storage impact."
- Cost: "We have calculated that including the start and end offsets will increase the size of the standard library’s pyc files by 22% (6MB) from 28.4MB to 34.7MB."
- Opt-out: "A new environment variable: `PYTHONNODEBUGRANGES`." / "A new command line option for the dev mode: `python -Xno_debug_ranges`." — "If any of these methods are used, the Python compiler will **not** populate code objects with the new information (`None` will be used instead) ... Additionally, the traceback machinery will not show the extended location information even if the information was present."

The 3.11 "What's New" (https://docs.python.org/3/whatsnew/3.11.html) shows the end result in a multi-frame trace, e.g. `return abs(point_1.x - point_2.x) + abs(point_1.y - point_2.y)` with `^^^^^^^^^` under `point_2.x` before `AttributeError: 'NoneType' object has no attribute 'x'`, and also lists PEP 678 (`BaseException.add_note()` — "It can be used to enrich exceptions with context information that is not available at the time when the exception is raised. The added notes appear in the default traceback.").

### 3.2 Python 3.10 "Better error messages"

https://docs.python.org/3/whatsnew/3.10.html — enabled by the PEG parser: "These improvements were enabled by the new PEP 617 parser, which uses non LL(1) capacities to provide more accurate error reporting." (summarised wording of the page's closing sentence; examples below are verbatim)

- `SyntaxError: '{' was never closed` (instead of 3.9's `invalid syntax` at a later line)
- `SyntaxError: invalid syntax. Perhaps you forgot a comma?`
- `SyntaxError: expected ':'`
- `SyntaxError: cannot assign to attribute here. Maybe you meant '==' instead of '='?`
- `IndentationError: expected an indented block after 'if' statement in line 2`
- full-range highlighting: `^^^^^^^^^^^^^^^^^^^^` under the whole generator expression in `Generator expression must be parenthesized`
- `AttributeError: module 'collections' has no attribute 'namedtoplo'. Did you mean: namedtuple?`
- `NameError: name 'schwarschild_black_hole' is not defined. Did you mean: schwarzschild_black_hole?`
- Caveat the page attaches to both "Did you mean" features: it is implemented in `PyErr_Display()`, so "This won't work if `PyErr_Display()` is not called, which can happen in some REPLs like IPython."

### 3.3 Python 3.12 and 3.13

- 3.12 (https://docs.python.org/3/whatsnew/3.12.html): `NameError: name 'sys' is not defined. Did you forget to import 'sys'?`; `NameError: name 'blech' is not defined. Did you mean: 'self.blech'?`; `SyntaxError: Did you mean to use 'from ... import ...' instead?`; `ImportError: cannot import name 'chainmap' from 'collections'. Did you mean: 'ChainMap'?`
- 3.13 (https://docs.python.org/3/whatsnew/3.13.html): tracebacks are colourised by default, controlled by `PYTHON_COLORS`, `NO_COLOR`, `FORCE_COLOR`; a shadowing diagnosis appended to the message — `AttributeError: module 'random' has no attribute 'randint' (consider renaming '/home/me/random.py' since it has the same name as the standard library module named 'random' and prevents importing that standard library module)`; and keyword-argument suggestions — `TypeError: split() got an unexpected keyword argument 'max_split'. Did you mean 'maxsplit'?`

---

## 4. Ruby — third-party gems → default gems → a hook API

### 4.1 `did_you_mean` (Yuki Nishijima): external gem 2014 → default gem in Ruby 2.3 (2015-12-25)

- Release history (https://rubygems.org/gems/did_you_mean/versions): first releases "0.1.0 February 16, 2014", "0.2.0 February 23, 2014", …; "1.0.0 December 22, 2015" (three days before Ruby 2.3.0).
- README (https://github.com/ruby/did_you_mean): "Ruby 2.3 and later ships with this gem and it will automatically be `require`d when a Ruby process starts up. No special setup is required." Licence line: "Copyright (c) 2014-16 Yuki Nishijima." Covered exceptions: NameError, NoMethodError, KeyError, LoadError, NoMatchingPatternKeyError. Example: `NameError: undefined local variable or method `methosd' for main:Object` / `Did you mean?  methods` / `method`. Opt-out: `--disable-did_you_mean`; `#original_message` returns the undecorated text.
- Ruby 2.3.0 release notes (https://www.ruby-lang.org/en/news/2015/12/25/ruby-2-3-0-released/): "The did_you_mean gem shows the candidates on the `NameError` and `NoMethodError` to ease debugging."
- **How it hooks in** (`lib/did_you_mean.rb`, `lib/did_you_mean/core_ext/name_error.rb`, https://github.com/ruby/did_you_mean/tree/master/lib): `DidYouMean.correct_error(NameError, NameErrorCheckers)` etc. registers a spell-checker per exception class and does `error_class.prepend(Correctable)`. `Correctable` is version-conditional:
  ```ruby
  if Exception.method_defined?(:detailed_message)
    def detailed_message(highlight: true, did_you_mean: true, **)
      msg = super.dup
      return msg unless did_you_mean
      suggestion = DidYouMean.formatter.message_for(corrections)
      ...
      msg << suggestion
    end
  else
    def to_s
      msg = super.dup
      suggestion = DidYouMean.formatter.message_for(corrections)
      msg << suggestion if !msg.include?(suggestion)
      msg
    end
  end
  ```
  i.e. before Ruby 3.2 it monkey-patched `#to_s` (hence `#message`); from 3.2 it overrides `#detailed_message` and leaves `#message` alone.

### 4.2 `error_highlight` (Yusuke Endoh): default gem in Ruby 3.1 (2021-12-25)

- Ruby 3.1.0 release notes (https://www.ruby-lang.org/en/news/2021/12/25/ruby-3-1-0-released/): "A built-in gem, error_highlight, has been introduced. It includes fine-grained error location in backtraces:"
  ```
  $ ruby test.rb
  test.rb:1:in `<main>': undefined method `time' for 1:Integer (NoMethodError)

  1.time {}
   ^^^^^
  Did you mean?  times
  ```
  The NEWS file (https://github.com/ruby/ruby/blob/v3_1_0/NEWS.md) adds the nested example (`title = json[:article][:title]` with the caret under `[:article]` vs `[:title]` depending on which receiver was nil) and: "This feature is enabled by default. You can disable it by using a command-line option `--disable-error_highlight`." Ruby 3.1 shipped `error_highlight 0.3.0`; the 3.1 notes say it "currently supports only `NameError`" (summarised).
- README (https://github.com/ruby/error_highlight): "Ruby 3.1 will ship with this gem and it will automatically be `require`d when a Ruby process starts up." and "Note: This gem works only on MRI and requires Ruby 3.1 or later because it depends on MRI's internal APIs that are available since 3.1." The public probe API: "`ErrorHighlight.spot` ... Note that the argument must be a RubyVM::AbstractSyntaxTree::Node object that is created with `keep_script_lines: true` option (which is available since Ruby 3.1)." Custom renderers via `ErrorHighlight.formatter=` (an object responding to `message_for(spot)`).
- **Mechanism, from `lib/error_highlight/base.rb`** (https://github.com/ruby/error_highlight/blob/master/lib/error_highlight/base.rb): `ErrorHighlight.spot(exc)` takes `exc.backtrace_locations.first` (a `Thread::Backtrace::Location`), then
  ```ruby
  node = RubyVM::AbstractSyntaxTree.of(loc, keep_script_lines: true)
  ```
  i.e. the VM can map a backtrace location back to the AST node it was compiled from, and `keep_script_lines` keeps the source text with the AST so the snippet can be printed. For iseqs compiled by Prism it falls back to `node_id = RubyVM::AbstractSyntaxTree.node_id_for_backtrace_location(location)` and `Prism.parse_file(absolute_path).value.breadth_first_search { |node| node.node_id == node_id }`. `Spotter` then chooses `point_type: :name` (the method/variable name) or `:args` (for TypeError/ArgumentError) and returns `{first_lineno, first_column, last_lineno, last_column, snippet, script_lines}`. Documented limits in the source: single-line fragments only; highlighting is skipped if the snippet contains non-ASCII characters ("It requires some work to support Unicode (or multibyte) characters."); `eval`'d code and missing files return `nil`.
- **Hook, from `lib/error_highlight/core_ext.rb`**: `NameError.prepend(CoreExt)`; and "ErrorHighlight is enabled for TypeError and ArgumentError only when Exception#detailed_message is available. This is because changing ArgumentError#message is highly incompatible." With `detailed_message` available it defines `detailed_message(highlight: false, error_highlight: true, **)` returning `super + snippet`; otherwise it overrides `to_s`, coordinating with did_you_mean through a `SKIP_TO_S_FOR_SUPER_LOOKUP` marker constant ("See https://github.com/ruby/did_you_mean/pull/152").

### 4.3 Ruby 3.2 (2022-12-25): `Exception#detailed_message` — the hook API added for these gems

- NEWS for 3.2.0 (https://github.com/ruby/ruby/blob/v3_2_0/NEWS.md): "Exception#detailed_message has been added. The default error printer calls this method on the Exception object instead of #message. [[Feature #18564]]" The same release bundles `error_highlight 0.5.1`.
- Release notes (https://www.ruby-lang.org/en/news/2022/12/25/ruby-3-2-0-released/): error_highlight "Now it points at the relevant argument(s) for TypeError and ArgumentError":
  ```
  test.rb:2:in `+': nil can't be coerced into Integer (TypeError)

  sum = ary[0] + ary[1]
                 ^^^^^^
  ```
- The rationale, Feature #18564 by mame/Yusuke Endoh, opened 2022-02-01 (https://bugs.ruby-lang.org/issues/18564): did_you_mean and error_highlight each "overrides `Exception#to_s` to add their suggestions", which "may break some tests to check the result of `Exception#to_s`" and "Some Ruby scripts re-raise an exception by `raise e.class, e.message, e.backtrace`, which makes the gems add their suggestion multiple times." The fix: "This proposal allows the gems to override `Exception#detailed_message`. `Exception#to_s` is kept as-is, so the above problems will no longer occur." The default error printer now calls `#detailed_message(highlight: Exception.to_tty?)`.

**Why this trio matters as a precedent:** gem published on rubygems (2014) → adopted as a default gem with the language (2.3, 3.1) → the core API grew an explicit extension point (3.2) once the monkey-patch approach caused real breakage. The gems remain separately versioned repos under `ruby/` and can be disabled per-process (`--disable-did_you_mean`, `--disable-error_highlight`).

---

## 5. Racket and the research literature

### 5.1 Marceau, Fisler, Krishnamurthi — "Mind Your Language: On Novices’ Interactions with Error Messages" (Onward! 2011)

Page + PDF: https://cs.brown.edu/~sk/Publications/Papers/Published/mfk-mind-lang-novice-inter-error-msg/ (DOI 10.1145/2048237.2048241). Abstract (from the page): "Error messages typically contain both a textual description of the problem and an indication of where in the code the error occurred. This paper reports on a series of studies that explore beginning students’ interactions with the vocabulary and source-expression highlighting in DrRacket. Our findings demonstrate that the error message significantly fail to convey information accurately to students, while also suggesting alternative designs that might address these problems."

Findings (verbatim from the PDF):
- "students struggle with the carefully-designed vocabulary of the error messages and often misinterpret the source highlighting."
- Interviews: "All three of these excerpts suggest that students initially interpreted the highlighting as saying “edit here”, or “look inside here for the problem”."
- "This suggests that vocabulary and message grammar are perhaps bigger factors than highlighting in students’ difficulties with responding to errors."
- On highlights that don't match the text: "If we hope to teach students guidelines for working with error messages such as “figure out what term has been highlighted”, we must ensure that our error messages only highlight such referents."
- The "over-focusing effect": "the current DrRacket error messages highlight the expression for the usage, but not the definition. Such errors can be difficult to fix when the problem lies in the definition instead. The highlight effectively has an over-focusing effect".

Section 7 "Recommendations" — two principles and four proposals (verbatim headings):
- "Error messages should not propose solutions. Even though some errors have likely fixes ..., those fixes will not cover all cases. Given students‘ tendencies to view DrRacket as an oracle, proposed solutions could lead them down the wrong path. This principle directly contradicts requests of the students we interviewed".
- "Error messages should not prompt students towards incorrect edits."
- "7.1 Simplify the vocabulary in the error messages." (e.g. "DrRacket uses the term “identifier” rather than “variable”, even though “variable” is the term students are used to").
- "7.2 Be explicit in errors about inconsistencies." (use vs definition; "An IDE cannot be omniscient about which of the definition or use is incorrect ... It can, however, avoid biasing a student towards one of the two").
- "7.3 Help students match message terms to code fragments." — "We therefore propose that error messages highlight every definite reference and its corresponding code with a distinct color."
- "7.4 Treat error messages as an integral part of course design."
- Generality claim: "Our recommendations about color-coded highlights, consistent vocabulary, non-biased error messages, and pedagogy are not specific to Racket."

Note the direct tension with Elm/Python/Ruby: this novice-focused work argues *against* proposing fixes; Rust's guide merely argues against phrasing fixes as questions.

### 5.2 Marceau, Fisler, Krishnamurthi — "Measuring the Effectiveness of Error Messages Designed for Novice Programmers" (SIGCSE 2011)

https://cs.brown.edu/~sk/Publications/Papers/Published/mfk-measur-effect-error-msg-novice-sigcse/ (PDF fetched). Abstract: "Good error messages are critical for novice programmers. Recognizing this, the DrRacket programming environment provides a series of pedagogically-inspired language subsets with error messages customized to each subset. We apply human-factors research methods to explore the effectiveness of these messages. ... we study messages at a fine-grained level by analyzing the edits students make in response to various classes of errors. We present a rubric (which is not language specific) to evaluate student responses".

The rubric (verbatim codes): "[DEL] Deletes the problematic code wholesale." "[UNR] Unrelated to the error message, and does not help." "[PART] Evidence that the student has understood the error message (though perhaps not wholly) and is trying to take an appropriate action (though perhaps not well)." "[DIFF] Unrelated to the error message, but it correctly addresses a different error or makes progress in some other way." "[FIX] Fixes the proximate error (though other cringing errors might remain)." Design stance: "The rubric should assess the performance of the error messages, not the students." Findings: "we expect a student to respond poorly to 3.16 errors in Lab #1 and 5.51 errors in Lab #2 ... a poorly-handled error message every 10 minutes"; "Based on other data (from interviews with students and a vocabulary quiz), we have good reason to suspect problems with the highlights and the vocabulary." Also a methodological warning relevant to anyone mining error logs: "the error message that a student sees is often not a direct indicator of the underlying error".

### 5.3 Becker et al. — "Compiler Error Messages Considered Unhelpful: The Landscape of Text-Based Programming Error Message Research" (ITiCSE-WGR 2019)

DOI https://doi.org/10.1145/3344429.3372508 (ACM page and ResearchGate both returned 403 to this session; the abstract below is from Semantic Scholar's record https://api.semanticscholar.org/graph/v1/paper/DOI:10.1145/3344429.3372508, the guideline wording from a WG21 paper that reproduces it, see below). Authors: Becker, Denny, Pettit, Bouchard, Bouvier, Harrington, Kamil, Karkare, McDonald, Osera, Pearce, Prather.

Abstract (verbatim): "Diagnostic messages generated by compilers and interpreters such as syntax error messages have been researched for over half of a century. Unfortunately, these messages which include error, warning, and run-time messages, present substantial difficulty and could be more effective, particularly for novices. ... We finally present a set of guidelines, curated from the literature, classified on the type of evidence supporting each one (historical, anecdotal, and empirical). ... We also make the bibtex file of our 300+ reference corpus publicly available."

The ten guidelines, as quoted by WG21 paper P2429R0 "Concepts Error Messages for Humans" (https://www.open-std.org/jtc1/sc22/wg21/docs/papers/2022/p2429r0.pdf, which cites the Becker paper directly): "Increase readability by using plain language, being concise, and writing errors for humans rather than tools." "Reduce cognitive load by placing relevant information near the offending code, reducing redundancy so the user does not process the same information twice, and using multiple modalities to provide feedback." "Provide context that can help the user." "Use a positive tone" ("when novices encounter violent messages ..., vague phrases ..., or obscure codes ..., they are understandably shaken, confused, dismayed, and discouraged from continuing"). "Show examples of similar errors that are minimal and aid understanding." "Show solutions or hints." "Allow dynamic interaction by providing the user with autonomy over error message presentation." "Provide scaffolding that helps the user connect concepts in the language with errors in their code." "Use logical argumentation by providing a coherent narrative of the error." "Report errors at the right time by giving the user the right amount of information when they need it." The same ten headings appear in the independent summary at https://neverworkintheory.org/2021/09/02/compiler-error-messages-considered-unhelpful.html. (P2429R0 also explicitly points to Elm — quoting "Compilers should be assistants, not adversaries" — and Rust as models for C++.)

### 5.4 Barik et al. — "Do Developers Read Compiler Error Messages?" (ICSE 2017)

PDF https://static.barik.net/barik/publications/icse2017/PID4655707.pdf (DOI 10.1109/ICSE.2017.59). Abstract, verbatim: "we conducted an eye tracking study with 56 participants from undergraduate and graduate software engineering courses at our university. The participants attempted to resolve common, yet problematic defects in a Java code base within the Eclipse development environment. We found that: 1) participants read error messages and the difficulty of reading these messages is comparable to the difficulty of reading source code, 2) difficulty reading error messages significantly predicts participants’ task performance, and 3) participants allocate a substantial portion of their total task to reading error messages (13%–25%). The results of our study offer empirical justification for the need to improve compiler error messages for developers." Also: "participants spent 65%–80% of their fixations in the source editor, with 13%–25% of their fixations on error message areas of interest." Design implication offered: "Analogous to Quick Fix popups, perhaps toolsmiths should offer “Quick Understanding” popups, which recommend appropriate tools ... that are known to be helpful in understanding a particular compiler error."

### 5.5 Clojure-specific research (Machkasova et al., University of Minnesota Morris)

- "Usability of beginner-oriented Clojure error messages", Fellows, Hagen, Stockholm, Machkasova, TFPIE 2016 (slides: https://wiki.tfpie.science.ru.nl/images/6/6e/TFPIE16-slides-emachkasova.pdf). Problem statement: "Errors are Java exceptions, are phrased in terms of Java types: `>(+ "hi")` `java.lang.ClassCastException: Cannot cast java.lang.String to java.lang.Number`". Their rewrite of `(+ 1 "hi")`: "Error: In function +, the second argument "hi" must be a number but is a string." with the ~25-line trace cut to three Ruby-user-level frames. Approach: "Can’t just surround the program with try/catch: compilation errors and lazy sequences evaluation errors escape to REPL"; "Overwrite core functions to add pre-conditions: + takes numbers, first takes a sequence, etc. Can print arguments that fail preconditions"; "Catch error messages and transform them using regular expression". Study: 11 participants, Racket vs standard Clojure vs modified Clojure. "Preliminary conclusions: Standard Clojure messages make it difficult to solve all questions. Our modified messages make results closer to Racket ... The stack trace in standard Clojure is uninformative and overwhelming, makes it hard to find errors".
- The follow-on tool, **Babel** (https://github.com/Clojure-Intro-Course/babel): "A proof-of-concept tool for transforming Clojure's error messages into beginner-friendly forms." — installed as nREPL middleware (`:repl-options {:nrepl-middleware [babel.middleware/interceptor]}`), i.e. it rewrites errors *outside* the language, at the REPL transport.
- 2025 update: Walbran, Stanislowski, Kalvoda, "Breaking Down Terminology of Clojure Error Messages for Beginner Programmers", UMM Undergraduate Research Symposium 2025 (https://digitalcommons.morris.umn.edu/urs_2025/6): "The terminology and shorthands used to convey necessary information for understanding the error are often confusing to novices." … Babel "replaces the language's default error messages with less jargon-heavy forms." (quotes as extracted by the fetch tool from the landing page).
- Context from a Clojure-dialect implementer: jank's error-reporting post (https://jank-lang.org/blog/2025-03-28-error-reporting/) states "In the yearly State of Clojure survey, error reporting is consistently at the top of areas where improvement is desired."

---

## 6. Briefly: Elixir, Gleam, and JVM-hosted languages

**Elixir.** Suggestions arrived incrementally through the standard library: 1.3.0 — "[Exception] Suggest possible functions on `UndefinedFunctionError` for existing modules"; 1.7.0 — "[Exception] Provide "did you mean" suggestions on KeyError"; 1.16.0 — "[Kernel] Suggest module names based on suffix and casing errors when the module does not exist in `UndefinedFunctionError`" (https://github.com/elixir-lang/elixir/blob/v1.3.0/CHANGELOG.md, …/v1.7.0/…, …/v1.16.0/…). The compiler side was rebuilt in 1.15–1.16: 1.15 "Introduce mechanism to collect several errors in a module. Previously, as soon as there was a compilation error, compilation would fail. Now the compiler ... will report multiple errors whenever possible as multiple `error: ...` messages, similar to `warning: ...`" plus `Code.with_diagnostics/2` "so this information can be leveraged by editors" (https://github.com/elixir-lang/elixir/blob/v1.15.0/CHANGELOG.md); 1.16 "we also include code snippets in exceptions and diagnostics raised by the compiler", e.g.
```
** (MismatchedDelimiterError) mismatched delimiter found on lib/my_app.ex:1:18:
    error: unexpected token: )
    │
  1 │ [1, 2, 3, 4, 5, 6)
    │ │                └ mismatched closing delimiter (expected "]")
    │ └ unclosed delimiter
    │
    └─ lib/my_app.ex:1:18
```
"When possible, we will show precise spans, such as on undefined variables ... Otherwise the whole line is underlined" (https://elixir-lang.org/blog/2023/12/22/elixir-v1-16-0-released/). 1.17 added set-theoretic type warnings for a listed set of cases (misspelled struct keys in patterns/access/update, calling functions on non-modules, structural comparisons on structs, etc.): "While some of these warnings were emitted in the past, they were discovered using syntax analysis. The new warnings are more reliable, precise, and with better error messages." and "These warnings also work natively in text editors, as they are standard Elixir compiler warnings" (https://elixir-lang.org/blog/2024/06/12/elixir-v1-17-0-released/; the actual warning text on that page is only in images). 1.18: "Our goal is for the warnings to provide enough contextual information that lead to clear reports." (https://elixir-lang.org/blog/2024/12/19/elixir-v1-18-0-released/); 1.20 (2026-06-03) describes the checker as reporting only "verified bugs: typing violations that are guaranteed to fail at runtime if executed" (https://elixir-lang.org/blog/2026/06/03/elixir-v1-20-0-released/, quote as extracted by the fetch tool).

**Gleam.** The FAQ says only that "Gleam's developer tooling were inspired in-part by Rust's." (https://gleam.run/frequently-asked-questions/); the home page (https://gleam.run/) promises "clear error messages" and shows:
```
error: Unknown record field

  ┌─ ./src/app.gleam:8:16
  │
8 │ user.alias
  │     ^^^^^^ Did you mean `name`?

The value being accessed has this type:
    User

It has these fields:
    .name
```
Architecture, verified in source: `compiler-core/src/diagnostic.rs` defines `pub struct Diagnostic { pub title: String, pub text: String, pub level: Level, pub location: Option<Location>, pub hint: Option<String> }` where `Location { src, path, label: Label{text, span}, extra_labels }`; `write_span` converts these to `codespan_reporting::diagnostic::Diagnostic` with `LabelStyle::Primary`/`Secondary` labels and emits via `codespan_reporting::term`, then appends the prose `text` and a wrapped `Hint: …` line (https://github.com/gleam-lang/gleam/blob/main/compiler-core/src/diagnostic.rs). `compiler-core/src/error.rs` (≈190 KB, one `Diagnostic` literal per error variant) has a `fn did_you_mean(name: &str, options: &[EcoString]) -> Option<String>` producing "Did you mean `{option}`?" by edit distance, used as the primary label text for unknown imports/fields (https://github.com/gleam-lang/gleam/blob/main/compiler-core/src/error.rs). So Gleam = Rust-style layout (via codespan-reporting) + Elm-style title/prose/hint.

**JVM-hosted dynamic languages and stack-trace noise.**
- *JRuby* rebuilds Ruby-level backtraces from the JVM stack: "In order to produce a Ruby trace that only contains lines interesting to a Ruby user, we reconstruct that trace using a combination of JVM stack trace frames and our per-thread, artificially-maintained interpreter frames. Backtrace-building walks the JVM trace backward, including only: Non-JRuby Java frames, such as from calling a Java library from Ruby; JRuby methods known to map to Ruby core methods (Kernel#eval, String#split, and so on); Already-jitted Ruby frames ...; Specially-named interpreter frames, replaced by the appropriate interpreter frame" (https://github.com/jruby/jruby/wiki/BacktraceGeneration, which shows the same error as a 7-line Ruby trace vs a ~60-line raw JVM trace). The switch is defined in `core/src/main/java/org/jruby/util/cli/Options.java`: `BACKTRACE_STYLE = string(MISCELLANEOUS, "backtrace.style", new String[]{"normal", "raw", "full", "mri"}, "normal", "Set the style of exception backtraces.")` and `BACKTRACE_MASK = bool(MISCELLANEOUS, "backtrace.mask", false, "Mask .java lines in Ruby backtraces.")`; `TraceType.traceTypeFor` maps `raw`→`Gather.RAW`, `normal`/`ruby_framed`→`Gather.NORMAL`, `full`→`Gather.FULL`, `mri`→MRI formatting (https://github.com/jruby/jruby/blob/master/core/src/main/java/org/jruby/runtime/backtrace/TraceType.java). Filtering is the default; the full JVM trace is one flag away.
- *Groovy* ships `org.codehaus.groovy.runtime.StackTraceUtils`: "Utility methods removing internal lines from stack traces"; `sanitize(Throwable)` — "Remove all apparently groovy-internal trace entries from the exception instance. This modifies the original instance and returns it, it does not clone"; `deepSanitize` — "Sanitize the exception and ALL nested causes" (https://docs.groovy-lang.org/latest/html/api/org/codehaus/groovy/runtime/StackTraceUtils.html). In source the filter is a package-prefix list from `System.getProperty("groovy.sanitized.stacktraces", "groovy.,org.codehaus.groovy.,java.,javax.,sun.,gjdk.groovy.,groovyjarjar,com.sun., …")`, frames are kept iff `isApplicationClass(className)`, and the unfiltered trace is logged at WARNING to a logger named `StackTrace` so nothing is lost (https://github.com/apache/groovy/blob/master/src/main/java/org/codehaus/groovy/runtime/StackTraceUtils.java). Unlike JRuby this is a library call, not the default printer.
- *Kotlin*: statically typed; nothing on runtime stack-frame filtering was fetched or verified for this report (see §8).

---

## 7. What the good ones have in common

Legend: ✔ = present by default; (opt) = available but opt-in/flag; — = absent or not found; n/a = not applicable. CT/RT marks which side. Evidence for each cell is in the sections above.

| Feature | Elm (CT) | Rust CT / RT | Python (RT) | Ruby (RT) | Elixir | Gleam (CT) | JRuby/Groovy (RT) |
|---|---|---|---|---|---|---|---|
| Source excerpt with caret/underline at the failing sub-expression | ✔ `Render/Code.hs` | ✔ `^^^`/`---` labels / — (file:line:col only) | ✔ PEP 657 `~~~^^^` (3.11) | ✔ error_highlight (3.1) | ✔ 1.16 diagnostics | ✔ codespan labels | — |
| Blame the user's code, not the library/runtime | ✔ (always the user's module) | ✔ / (opt) backtrace off by default; "read until you see files you wrote" | ✔ frames show user source | ✔ caret in user line | (not verified) | ✔ | ✔ JRuby default trace filters JVM frames; Groovy `sanitize()` |
| Plain-language explanation of *why* | ✔ "assistants, not adversaries" | ✔ message + notes; style guide | partial (specific messages; PEG parser) | partial | ✔ type-warning prose | ✔ `text` field | — |
| Concrete fix / hint | ✔ `Hint:` on every message | ✔ `help` + suggestions with `Applicability` (never phrased as a question) | ✔ "Did you mean" / "Did you forget to import" | ✔ did_you_mean | ✔ suggestions since 1.3/1.7 | ✔ `hint` field | — |
| Error codes + longer explanation | — (links to elm-lang.org docs via `makeLink`) | ✔ `E0499`, `--explain`, error index | — | — | — | — | — |
| Structured/JSON form for tools | ✔ `--report=json` (title/region/message) | ✔ `--error-format json` (spans, labels, suggestions, applicability, `rendered`) | (API: `co_positions`, `add_note`) | (API: `ErrorHighlight.spot`, `detailed_message`) | ✔ `Code.with_diagnostics/2` | — (not checked) | — |
| Consistent visual layout / colour with a no-colour path | ✔ ANSI iff TTY, 80-col plain | ✔ RFC 1644 format | ✔ 3.13 colour, `NO_COLOR` | ✔ `highlight:` kwarg | ✔ ANSI snippets | ✔ termcolor | — |
| Hide internal frames by default, opt-in to show | n/a (no runtime) | ✔ `RUST_BACKTRACE=1` | — (full traceback always) | — (full backtrace) | — (not verified) | n/a | ✔ `-Xbacktrace.style=raw`, `backtrace.mask`; Groovy property list |
| "Did you mean" spelling suggestions | ✔ typo detection for record fields | ✔ but worded as a statement, by policy | ✔ 3.10+ (names, attrs, imports, kwargs) | ✔ did_you_mean (2.3+) | ✔ | ✔ `did_you_mean()` in `error.rs` | — |
| Machine-applicable fixes | — | ✔ `MachineApplicable` → `rustfix` | — | — | — | — | — |
| Extension hook so third parties can decorate errors | — | crates (annotate-snippets etc.) for *other* languages | `PyErr_Display` (noted as the limitation) | ✔ `Exception#detailed_message` (3.2), gems prepend modules | — | — | JRuby `TraceType`; Groovy `addClassTest` |
| Corpus of bad messages driving iteration | ✔ error-message-catalog | ✔ (RFC + dev-guide process) | — | — | — | — | — |

Cross-cutting observations that the sources support directly:

1. **Lineage is explicit.** RFC 1644 lists "Draw inspiration from Elm" as a goal; Gleam says its tooling is "inspired in-part by Rust's" and literally links the Rust crate; P2429R0 (C++) cites Elm and Rust as the models.
2. **Every good renderer keeps the source.** Elm's `Module._source`, Rust's `spans[].text`, Ruby's `keep_script_lines`, Python's `co_positions` all exist so the printer can show the user's own line — Elm's first principle ("shows the code exactly as you wrote it").
3. **The runtime side is the hard one, and the dynamic languages solved it by adding per-instruction/per-node position tables** (Python `co_positions` at a measured 22 % pyc cost; Ruby node-id → AST via `RubyVM::AbstractSyntaxTree.of(backtrace_location)`), not by re-architecting exceptions.
4. **Adoption path (Ruby) = gem → default gem → core hook.** The core hook (`detailed_message`) was motivated by concrete breakage from monkey-patching `#to_s`.
5. **The research does not fully agree with the practitioners.** Marceau et al. argue messages "should not propose solutions" for novices; Rust only bans the *question* phrasing; Python/Ruby/Elixir/Gleam all embraced "Did you mean". Becker et al.'s ten guidelines ("Show solutions or hints", "Reduce cognitive load by placing relevant information near the offending code", "Use a positive tone", "Allow dynamic interaction") are the closest thing to a consensus list, and Barik et al. supply the empirical justification that people *do* read messages and that reading difficulty predicts fix success.

---

## 8. What I could not verify

- **Becker et al. 2019 primary text.** The ACM DL and ResearchGate copies returned HTTP 403; the abstract is from Semantic Scholar's record and the guideline wording is as reproduced in WG21 P2429R0 and neverworkintheory.org. The ten headings agree across both secondary sources, but the exact phrasing in the paper and its evidence classification per guideline were not read.
- **Elixir 1.17/1.20 type-warning text.** The blog posts show the warnings only as images; I have the changelog prose but not the literal warning format. Whether Elixir hides internal frames in runtime exceptions was not checked.
- **Ruby VM internals.** I verified the *API* used (`RubyVM::AbstractSyntaxTree.of(location, keep_script_lines: true)`, `node_id_for_backtrace_location`) from the gem source and README, not the C implementation that stores node ids in iseqs.
- **Rust `AnnotateSnippetEmitter` command-line flag.** The emitter file exists in `rustc_errors`; I could not confirm the current unstable flag name that selects it (a grep of `rustc_session/src/config.rs` for "annotate" found nothing).
- **Kotlin.** Not fetched at all; listed only as "statically typed, n/a" without a source.
- **Elm site pages.** The live elm-lang.org pages are JS bundles; quotes are from the page sources in github.com/elm/elm-lang.org, which is what the site renders.
- **Barik et al.** The fetch tool's own summary of the PDF contradicted the paper (it claimed developers *don't* read messages); it was discarded and all quotes were taken from the PDF text extracted locally.
- **Clojure UMM 2025 paper.** The PDF endpoint returned 403 to a direct download; quotes are from the fetch tool's extraction of the landing page abstract.
- **Groovy `groovy.sanitized.stacktraces`.** Verified from the Apache Groovy source on GitHub; the published Javadoc page fetched did not include the property description.
