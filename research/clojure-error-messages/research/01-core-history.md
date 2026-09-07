# History of error-message work in Clojure core, and community sentiment

Research date: 2026-09-06. Every claim below is tied to a URL that was actually fetched during this session (via `curl`, the JIRA REST API, the HN Algolia API, a `git clone` of `matthiasn/talk-transcripts`, or the WebFetch tool). Where a quote came only through the WebFetch summarizer (a lossy tool) rather than raw HTML I say "(WebFetch)". Things I could not verify are listed in the last section rather than guessed.

Raw downloads used for this report are in `scratchpad/research/raw/` (changes.md, survey posts, JIRA JSON, PDF text, transcripts).

---

## A. Community sentiment: is "error messages" a top complaint?

### A.1 The State of Clojure surveys, year by year

Survey naming: the survey is named for the year it was run; results are usually posted the following January–March (later in recent years). There is no "2017" survey post; the 2018 survey ran in January 2018. All results posts on clojure.org: https://clojure.org/news/news lists 2015, 2016, 2018, 2019, 2020, 2021, 2022, 2023, 2024, 2025.

| Survey | Results post | Was a "frustration/challenge" question reported? | Where "error messages" ranked |
|---|---|---|---|
| 2015 | https://www.cognitect.com/blog/2016/1/28/state-of-clojure-2015-survey-results (Justin Gehtland, 28 Jan 2016; mirrored at https://clojure.org/news/2016/01/28/state-of-clojure-2015) | Yes: *"What has been most frustrating or has prevented you from using Clojure more than you do now?"* | **#1** (first year the choice existed) |
| 2016 | https://clojure.org/news/2017/01/31/state-of-clojure-2016 (Justin Gehtland, 31 Jan 2017) | Yes, same question | **#1** |
| 2018 | https://clojure.org/news/2018/03/19/state-of-clojure-2018 (Alex Miller, 19 Mar 2018; also https://cognitect.com/blog/2017/1/31/clojure-2018-results) | Yes ("how Clojure and ClojureScript should change or be prioritized for improvement"; format changed) | **#1** ("The top result is clearly error messages") |
| 2019 | https://clojure.org/news/2019/02/04/state-of-clojure-2019 (Alex Miller, 4 Feb 2019) | **Not reported in the post** (grep of raw HTML: zero hits for `error`, `frustrat`, `challenge`) | n/a in post; free-text comments analysed by Daniel Compton (below) |
| 2020 | https://clojure.org/news/2020/02/20/state-of-clojure-2020 (Alex Miller, 20 Feb 2020) | Not reported | Only mention: 1.10.1 "was a maintenance release this year with error handling improvements" |
| 2021 | https://clojure.org/news/2021/04/06/state-of-clojure-2021 (Fogus, 6 Apr 2021) | Not reported (zero hits) | n/a |
| 2022 | https://clojure.org/news/2022/06/02/state-of-clojure-2022 (Robert Randolph, 2 Jun 2022) | Not reported (zero hits) | n/a |
| 2023 | https://clojure.org/news/2023/06/30/state-of-clojure-2023 (Alex Miller, 30 Jun 2023) | Yes, as an open-text PDF: "Open responses for Q7, what are your biggest challenges in using Clojure now" → https://download.clojure.org/stateofclojure/2023/Data_Q7_230630.pdf | Not ranked by the post; see my keyword count below |
| 2024 | https://clojure.org/news/2024/12/02/state-of-clojure-2024 (Alex Miller, 2 Dec 2024) | Yes, for **new users only**: chart "Which areas would you prioritize for Clojure improvements?" | **#1 at 48.62%** (new-user subset, n=181) |
| 2025 | https://clojure.org/news/2026/02/18/state-of-clojure-2025 (18 Feb 2026) | Not reported (zero hits for `error`, `challeng`, `priorit`) | n/a |

**2015 (verbatim, cognitect.com):**
> When asked "What has been most frustrating or has prevented you from using Clojure more than you do now?," "Hiring and Staffing" is the second most common answer (right after "Error Messages"). Fully 34% of all respondents, 36% of those saying they "use it at work," list this as a blocker to further adoption.

> "Error messages" is the obvious choice that sticks out here. This was also the first time this choice was included in the survey so we don't have any historical data for comparison. We expect to evaluate possible changes in this area in upcoming Clojure releases.

(The percentage for "Error messages" itself is only in a chart image; not verified.)

**2016 (verbatim, clojure.org):**
> As was true the last two years, error messages and "hiring and staffing" are the top 2 reasons given for "What has been most frustrating or has prevented you from using Clojure more than you do now?" though both have fallen several percent since then. Interestingly, "Need docs/tutorials" has jumped from #5 in 2015 to #3 now, which corresponds well with a continuing growth of new entrants into the community.

> Error messages continued to be the top frustration for people and we will continue to improve those with the integration of spec in Clojure 1.9. Interestingly, the majority of the other frustrations went down this year compared to last year: Hiring/staffing - from 33% to 30% / Scripting - from 33% to 18% (maybe due to the rise of Planck and Lumo) …

On the free-text question: "Other categories centered around expected themes (many are areas of current or future work): docs/tutorials, error messages, tooling, startup time, etc."

**2018 (verbatim, clojure.org, Alex Miller):**
> There are several questions about how Clojure and ClojureScript should change or be prioritized for improvement. The results are largely similar to prior years, although the question format changed a little making it hard to directly compare every detail. The top result is clearly error messages though - while spec has started us down a road, that is still a work in progress which will continue this year. Many people have been using the Expound library for taking spec error output and making the data easier to read.

**2019–2022:** the official posts do not discuss frustrations/challenges at all. Daniel Compton's independent analysis of the 2019 free-text comments (https://danielcompton.net/clojure-survey-2019, 6 Feb 2019, curl-verified):
> Error messages have been a top complaint in Clojure for a long time, I think since the very first survey. CLJ-2373 introduced improvements to error messages in Clojure 1.10. A number of comments complained about error messages this year, but none of the complaints mentioned 1.10's changes. Given that 1.10 was released on 17 December 2018, and the survey ran from 7-22 January 2019, it seems likely to me that many of the people complaining haven't tried the latest improvement to Clojure's error messages.

Respondent quotes he reproduced: "1.10 alleviates most of the problems I found with error messages. Since this was my single biggest concern I had with Clojure, I just want to say thank you for listening!" / "Just give me the Clojure filename and line where the exception happened, please. I beg of you." / "Better error messages. Please!" / "Great to see the improved error messages in Clojure 1.10 - it was my number one issue in all previous Clojure surveys, but not this one :-)" / (under Spec) "Spec made Clojure errors worse."

Alex Miller's response to that analysis, https://insideclojure.org/2019/02/07/survey-comments/ (curl-verified), addresses spec, docs, etc. and contains **no paragraph mentioning "error"** (0 hits).

**2023 (my keyword count of the official Q7 PDF, 971 responses parsed, "What are your biggest challenges in using Clojure now?", Answered 971 / Skipped 790):** responses containing "error": 58; containing "error message": 40; containing "stack trace"/"stacktrace": 23; error OR stacktrace: 77 (~8% of responses). For comparison: libraries/ecosystem 157, tooling/IDE/editor 111, hiring/jobs/employ 40, docs 39, debug 33, types/static 27, startup 25. Sample verbatim responses: "Unfortunately, still error messages" (#28), "Error messages. On the REPL I often get opaque errors that don't give me line numbers" (#119), "Error messages and exceptions are often cryptic and not helpful." (#142), "Error messages remain hard to read, especially spec validation errors" (#385), "Error messages are still pretty awful" (#428), "The error messages are absolutely inscrutable." (#457). Caveat: crude substring counting, not a coded analysis.

**2024 (chart image read directly, https://clojure.org/images/content/news/2024-12-02/new-priorities.png, introduced in the post by "Similarly, we can look just at priorities for new users:"):** "Which areas would you prioritize for Clojure improvements?" Answered: 181, Skipped: 45 — Error messages **48.62%**, Tutorials 41.44%, Startup time 35.36%, clj and tools.deps 30.39%, Reference docs 25.97%, Spec 22.65%, Runtime performance 19.34%, Memory management 12.71%, Core library 11.05%. The post then says: "These don't vary too much from the community as a whole, but spec is ranked quite a bit lower level." (The whole-community version of this chart is not in the post; see open questions.) Note the separate "Challenges in learning" chart (2022–2024 trend; https://clojure.org/images/content/news/2024-12-02/new-challenges.png) does **not** include error messages among its choices (choices: Idioms, Core lib, FP vs OO, Syntax/parens, REPL, Finding libs, Structured editing, Finding examples, Running programs, Immutable data/state, Namespaces/vars, Modeling data).

### A.2 Other sources of the complaint (all fetched)

- **Hacker News**, 30 May 2017, user hacker_9 on "Notes on debugging Clojure code" (https://news.ycombinator.com/item?id=14444093, via Algolia API): "I find debugging + error messages to be the worst part of Clojure."
- **Hacker News**, 28 Feb 2025, user KingMob on "Why Clojure?" (https://news.ycombinator.com/item?id=43203265): "If the core team had ever addressed the decade of surveys showing that error messages/stacktraces were people's top complaints, you wouldn't need Claude."
- **Hacker News**, 2 Mar 2017, user defenestration on "The Clojure Error Message Catalog" (https://news.ycombinator.com/item?id=13761898): "When I was learning Clojure a year ago, I spent quite some time Googling the cryptic error messages. An overview like this would have been useful. But I never really understood why Clojure couldn't give better error-messages by itself."
- **Clojure Error Message Catalog** (https://github.com/yogthos/clojure-error-message-catalog, README via WebFetch): "a catalog of common Clojure errors and their meanings"; "This project was inspired by the way Elm does this, where they have a repo for this purpose, and then provide custom compiler errors for these cases." 441 stars / 22 forks at fetch time.
- **Eric Normand, "Clojure Error Messages are Accidental"** (https://ericnormand.me/article/clojure-error-messages-accidental, 28 Jun 2018, WebFetch): "Clojure's core functions are, for the most part, implemented without checks on the arguments. They only code the 'happy path'." / "It's not so much that Clojure's errors are bad. It's more that they're accidental."
- **ClojureVerse, "Improving error messages in Clojure (as a library)?"** (https://clojureverse.org/t/improving-error-messages-in-clojure-as-a-library/1765, borkdude, 15 Mar 2018, WebFetch): borkdude: "usually because poor error messages. It's never been really a problem for me, but this has to become a priority if we want adoption." No core-team participation.
- **ClojureVerse, "Nice error messages?"** (https://clojureverse.org/t/nice-error-messages/10684, Apr 2024–Jan 2025, WebFetch): no core-team posts; didibus: "Clojure's are still worse, because the error reported is often the underlying Java one, and it is reported to you in Java parlance."
- **Lambda Island, "What Is Behind Clojure Error Messages?"** (https://lambdaisland.com/blog/2022-04-07-Clojure-Error-Messages, 7 Apr 2022, WebFetch): "What makes it hard is that Clojure error messages are not very informative." / "This is not just a junior thought, but one also common to more advanced programmers."
- **jank blog, "Can jank beat Clojure's error reporting?"** (https://jank-lang.org/blog/2025-03-28-error-reporting/, Jeaye Wilkerson, 28 Mar 2025, WebFetch): "In the yearly State of Clojure survey, error reporting is consistently at the top of areas where improvement is desired." / "This applies not only to 2024's results. It goes all the way back to the first 2015 results." (I could verify this for 2015, 2016, 2018 and 2024-new-users; the 2019–2023 official posts are silent and the SurveyMonkey pages are not fetchable — see open questions.)
- **Stuart Halloway himself**, Strange Loop 2018 "Running With Scissors" (transcript https://github.com/matthiasn/talk-transcripts/blob/master/Halloway_Stuart/RunningWithScissors.md, cloned): "…the printing actually does not show you Clojure's error messages, which are somewhat famous, shall we say."
- **Academic/teaching**: Clojure/conj 2018 had a talk "Babel: middleware for beginner-friendly error messages" by Elena Machkasova, Charlot Shaw, Ethan Uphoff, RJ Holman (verified on http://2018.clojure-conj.org/speakers/). Clojure/conj 2015: Colin Fleming, "Improving Clojure's Error Messages with Grammars" (https://www.youtube.com/watch?v=kt4haSH2xcs, cited on the core design-wiki "Macro Grammars" page below).
- **Daniel Compton 2015 analysis** (https://danielcompton.net/clojure-survey-2015, WebFetch) reproduces a respondent: "…please make error messages better. I know error messages continue to receive this sort of 'well, if you wanna make 'em better, patches welcome…' treatment, but I think it's a mistake to keep beating up on them."

---

## B. What the core team has shipped, by version

Primary source for all version claims: https://raw.githubusercontent.com/clojure/clojure/master/changes.md (downloaded; 3194 lines). Ticket details from https://clojure.atlassian.net/rest/api/2/issue/CLJ-nnnn.

### B.0 Pre-history (for context)
changes.md has an explicit error-message section in almost every release since 1.5: 1.5 "4 Improved error messages" (CLJ-103, 897, 788 "Add source and line members and getters to CompilerException", 157, 940, 1052); 1.6 "3.2 Error messages" (CLJ-1248, 1099 "If non-seq passed where seq is needed, error message now is an ExceptionInfo with the instance value, retrievable via ex-data", 1083, 1056, 1102, 939); 1.7 "3.1 Error messages" (CLJ-1261, 1297) plus "2.7 Printing as data" which introduced `Throwable->map` and the `#error {...}` print form; 1.8 "2.1 Error handling" (CLJ-1778, 1456, 1282, 1210).

### B.1 Clojure 1.9 (8 Dec 2017) — specs on core macros
Release post https://clojure.org/news/2017/12/08/clojure19 (Alex Miller) lists spec as supporting "Validation, Error reporting, Destructuring, Instrumentation, …" and introduces "core.specs.alpha - specifications for Clojure itself". changes.md §2.1 verbatim:
> **2.1 Spec syntax checking** — If a macro has a spec defined via fdef, that spec will be checked at compile time. Specs have been defined for many clojure.core macros and errors will be reported for these based on the specs at compile time.

Also in 1.9: "`clojure.core/Throwable->map` formerly returned `StackTraceElement`s … Now the StackTraceElements are converted to data such that the return value is pure Clojure data, as intended." and CLJ-2184 "Propagate meta in doto forms to improve error reporting".

**What it did to error messages.** The 1.9 message format embedded the whole spec explain output *and* the raw explain-data map in the exception message. The core design wiki's own "Message in 1.9" example (see B.5) is:
> `CompilerException clojure.lang.ExceptionInfo: Call to clojure.core/let did not conform to spec: In: [0] val: () fails spec: :clojure.core.specs.alpha/bindings at: [:args :bindings :init-expr] predicate: any?, Insufficient input #:clojure.spec.alpha{:problems [...], :spec #object[clojure.spec.alpha$regex_spec_impl$reify__2436 0x4b45dcb8 ...], :value ([x]), :args ([x])}, compiling:(foo.clj:10:5)`

Community reaction (Daniel Compton 2018, curl-verified): "this year they have gotten worse with the introduction of specs on core macros." Respondent: "I was excited about spec initially; however, I find the error messages so large, noisy and unhelpful that I don't want to use it." Alex Miller in the survey post: "while spec has started us down a road, that is still a work in progress". Alex Miller on Clojurians Slack #beginners, 16 Oct 2018 (https://clojurians-log.clojureverse.org/beginners/2018-10-16, WebFetch), about `ns` spec failures: "rather than telling you that it expected one of the N options, it says that it didn't match anything and found "extra input", which is true but not helpful." and "my biggest takeaway from spec'ing ns, is that if we had spec when ns was written, we would have used a different dsl for it b/c it sucks".

### B.2 Clojure 1.10 (17 Dec 2018) — the phases/ex-triage release
Release post https://clojure.org/news/2018/12/17/clojure110 (Alex Miller), verbatim:
> Error reporting at the REPL now categorizes errors based on their phase of execution (read, macroexpand, compile, etc). Errors carry additional information about location and context as data, and present phase-specific error messages with better location reporting. This functionality is built into the clojure.main REPL, but the functionality is also available to other REPLs and tools with the ability to use and/or modify the data to produce better error messages.

changes.md "2.1 Error messages" (features section), verbatim:
> Clojure errors can occur in several distinct "phases" - reading source, macroexpansion, compilation, execution, and result printing. Clojure (and the REPL) now identify these phases in the exception and the message.
>
> The read/macroexpand/compile phases produce a CompilerException and indicate the location in the caller source code where the problem occurred (previously macroexpansion reported the error in the macroexpansion stack). CompilerException now implements IExceptionInfo and ex-data will report exception data including the following (optional) keys:
> * :clojure.error/phase - phase (:read-source, :macro-syntax-check, :macroexpansion, :compile-syntax-check, :compilation, :execution, :read-eval-result, :print-eval-result)
> * :clojure.error/source - source file
> * :clojure.error/line - line in source file
> * :clojure.error/column - column of line in source file
> * :clojure.error/symbol - symbol being macroexpanded or compiled
> * :clojure.error/class - cause exception class symbol
> * :clojure.error/cause - cause exception message
> * :clojure.error/spec - explain-data for spec errors
>
> clojure.main also contains two new functions: `ex-triage` and `ex-str` that can be used by external tools to mimic some or all of the Clojure repl reporting. `ex-triage` takes the output of `Throwable->map` and produces a concise analysis of the error phase, cause, etc (same keys as above). `ex-str` takes that analysis data and produces a message to print at the repl.

Tickets listed under that heading: CLJ-2373 "Detect phase and overhaul exception message and printing"; CLJ-2415 "Error cause should always be on 2nd line of error message"; CLJ-2420 "Refinement of error phases, `ex-triage`, execution error line reporting"; CLJ-2427 "CompilerException.toString() can throw if making message during initialization"; CLJ-2430 "Elevate phase in throwable data and conveyance for prepl"; CLJ-2435 "Include root cause class name in compilation and macroexpansion error phases"; CLJ-2438 "Demunge source symbol in execution error messages".

changes.md "3.1 Error messages" (enhancements): CLJ-1279 "Report correct arity count for function arity errors inside macros"; CLJ-2386 "Omit ex-info construction frames"; CLJ-2394 "Warn in pst that stack trace for syntax error failed before execution"; CLJ-2396 "Omit :in clauses when printing spec function errors if using default explain printer"; CLJ-1797 "Mention cljc in error when require fails"; CLJ-1130 "Improve error message when unable to match static method". Also CLJ-1209 "Print ex-data in clojure.test error reports", CLJ-2349 "Report correct line number for uncaught ExceptionInfo in clojure.test", and new `ex-cause`/`ex-message` "added to match existing functions in ClojureScript to increase the portability of error-handling code".

**The JIRA tickets (REST API, verified):**
- **CLJ-2373** "improvements to exception messages and printing" — reporter **Stuart Halloway**, created 2018-07-09, resolved 2018-09-05, fix 1.10, labels error-reporting/errormsgs/spec. Its description is the closest thing to a design document; full text in C.1 below. It links "Also see: https://dev.clojure.org/display/design/Exception+handling+update".
- **CLJ-2380** — *not* an error ticket ("read/print handlers for inst/java.util.Date inconsistent between different JDKs", Ghadi Shayban, Open). The task prompt's guess was wrong.
- **CLJ-2386** "In ex-info, avoid recording stack frames of ex-info itself" — Alex Miller, 2018-08-16 → 2018-09-05, fix 1.10.
- **CLJ-2415** — Alex Miller, 2018-10-08 → 2018-10-09: "After the changes in 1.10 alphas, the cause is sometimes, but not always on the 2nd line of the error message. This should be standardized so the cause message is always on the 2nd line." Contains before/after samples such as `Syntax error macroexpanding clojure.core/let at (2:1). Cause: Call to clojure.core/let did not conform to spec.`
- **CLJ-2420** "Refinement of Clojure error phases and location reporting" — Alex Miller, 2018-10-31 → 2018-11-06. Problems: "Refine error phases to separate "syntax checking" from the phase itself"; "Use phase names that better reflect the actual location of the error (ie :read-source rather than :read)"; "Break ex-str into two parts: ex-triage (semantic analysis of Throwable data) and ex-str (string-making) to increase options for reuse by other tools"; "For execution errors, report the line in the stack reflecting the user operation (outside core), rather than in the guts of core (RT.java, etc)". Note in patch: "currently we bucket all compile errors into :compile-syntax-check. later, we can refine compiler errors and separate some into :compilation."
- **CLJ-2430** — Alex Miller, 2018-11-15 → 2018-11-16: "phase is currently buried in ex-data of top wrapper exception"; "error phases are not currently documented"; "Elevate phase to top-level :phase key in Throwable->map"; "Make ReaderException an IExceptionInfo".
- **CLJ-2435** — Alex Miller, 2018-11-19: "Include the root class name unless it is RuntimeException or Exception, which don't add anything." e.g. `Syntax error (ClassNotFoundException) compiling at (REPL:0:0).`
- **CLJ-2438** — Alex Miller, 2018-11-20 → 2018-12-05: demunge `user$f/invokeStatic` → `user/f` in execution errors.

**What the REPL actually printed before vs after (source verified):** 1.9 `clojure.main/repl-caught` (https://raw.githubusercontent.com/clojure/clojure/clojure-1.9.0/src/clj/clojure/main.clj) printed `(.getSimpleName class) " " (.getMessage ex)` plus the *top stack element only* (never a full trace). 1.10 (https://raw.githubusercontent.com/clojure/clojure/clojure-1.10.0/src/clj/clojure/main.clj) replaced it with `(print (-> e Throwable->map ex-triage ex-str))`. So the REPL did not "stop printing stack traces" in 1.10 — it never printed full traces; it replaced the one Java frame with a phase + user-source location. `ex-str` docstring (1.10): "The first line summarizes the exception phase and location. The subsequent lines describe the cause."

**Reference documentation** (https://clojure.org/reference/repl_and_main, curl-verified), section "Error printing → At REPL", verbatim:
> As of Clojure 1.10, Clojure errors categorized into one of several phases:
> * :read-source - an error thrown while reading characters at the REPL or from a source file.
> * :macro-syntax-check - a syntax error found in the syntax of a macro call, either from spec or from a macro throwing IllegalArgumentException, IllegalStateException, or ExceptionInfo.
> * :macroexpansion - all other errors thrown during macro evaluation are categorized as macroexpansion errors.
> * :compile-syntax-check - a syntax error caught during compilation.
> * :compilation - non-syntax errors caught during compilation.
> * :execution - any errors thrown at execution time.
> * :read-eval-result - any error thrown while reading the result of execution (only applicable for REPLs that read the result).
> * :print-eval-result - any error thrown while printing the result of execution.
>
> Exceptions thrown during all phases (exception :execution) will have ex-data attached with one or more of the following keys: :clojure.error/phase - phase indicator; :clojure.error/source - file name (no path); :clojure.error/line - integer line number; :clojure.error/column - integer column number; :clojure.error/symbol - symbol being expanded/compiled/invoked; :clojure.error/class - cause exception class symbol; :clojure.error/cause - cause exception message; :clojure.error/spec - explain-data for a spec error
>
> The clojure.main REPL includes the categorization and printing of errors by default, but the individual steps of this process are exposed as well for other REPLs to use, specifically the functions: Throwable->map - converts an Exception chain into Clojure data; ex-triage - analyzes Clojure exception data to pull relevant information from the top and bottom of the exception chain into a map describing just the set of data needed to format an exception string; ex-str - produces a phase-appropriate message given a set of exception data
>
> The clojure.main REPL combines these functions in a pipeline to produce the printed exception message: (-> ex Throwable->map clojure.main/ex-triage clojure.main/ex-str). Other REPLs can use one or more pieces of this pipeline as necessary when building or customizing their exception printing.

**Alex Miller's write-up** (https://insideclojure.org/2018/12/17/errors/, "Clojure 1.10 error messages", 17 Dec 2018, curl-verified):
> One of the things I spent the most time working on for Clojure 1.10 (along with Rich and Stu) is updated error messages. The thing we spent the most time working through was identifying the different phases of execution, how to distinguish them, and what to say when we found them.

> …all exceptions occurring during read, macroexpansion, or compilation that are syntax-related are noted as "syntax" errors, indicating that something is wrong with the user's code, not an error during execution.

> Based on a review of clojure.core and many open source macros, it was determined that the vast majority of explicit syntax checks in macro implementations were throwing IllegalArgumentException, IllegalStateException, or ExceptionInfo. These exception types, when throw from a macro, will be treated as :macro-syntax-check phase.

> Previously, all of this location data was stored only embedded in the error message string. Now, the exception being thrown from the compiler contains that information as ex-data for use by tools

> We spent some extra time on filtering unnecessary stack frames at the top and demunging the Java frame to report the top Clojure function in the stack

> This is still only scratching the surface of all the changes that were made

Before/after examples from that post: 1.9 `RuntimeException Invalid token: :::5 clojure.lang.Util.runtimeException (Util.java:221)` → 1.10 `Syntax error reading source at (REPL:2:0). / Invalid token: :::5`; 1.9 `IllegalArgumentException cond requires an even number of forms clojure.core/cond (core.clj:600)` → 1.10 `Syntax error macroexpanding cond at (REPL:1:1). / cond requires an even number of forms`; `(let [x])` → `Syntax error macroexpanding clojure.core/let at (REPL:1:1). / [x] - failed: even-number-of-forms? at: [:bindings] spec: :clojure.core.specs.alpha/bindings`; `(/ 1 0)` → `Execution error (ArithmeticException) at user/eval144 (REPL:1). / Divide by zero`.

Contemporary confirmation that Stu and Alex were the workers (Eric Normand newsletter 283, 16 Jul 2018, https://ericnormand.me/issues/purelyfunctional-tv-newsletter-283-spec-error-messages-games, WebFetch): "Stuart Halloway and Alex Miller are working on improving error messages in Clojure." (linking CLJ-2373).

### B.3 Clojure 1.10.1 (6 Jun 2019) — launcher error reporting, `-Dclojure.main.report`
Release post https://clojure.org/news/2019/06/06/clojure1-10-1 (curl-verified): "Clojure 1.10.1 is a small release focusing on two issues: working around a Java performance regression and improving error reporting from clojure.main." changes.md §1.2 verbatim:
> **1.2 clojure.main Error Reporting** — clojure.main is frequently used as a Clojure program launcher by external tools. Previously, uncaught exceptions would be automatically printed by the JVM, which would also print the stack trace.
>
> This release will now catch exceptions and use the same error triage and printing functionality as the Clojure repl. The full stack trace, ex-info, and other information will be printed to a target specified by the configuration.
>
> The three available error targets are: file - write to a temp file (default, falls back to stderr); stderr - write to stderr stream; none - don't write
>
> These error targets can be specified either as options to clojure.main, or as Java system properties (flags take precedence). When invoking clojure.main (or using the clj tool), use `--report <target>`. For Java system property, use `-Dclojure.main.report=<target>`.

Tickets: CLJ-2463 "Improve error printing in clojure.main with -m, -e, etc" (Alex Miller, 2019-01-04 → 2019-03-22); CLJ-2497 "Put error report location on its own line"; CLJ-2504 "Provide more options for error reporting in clojure.main"; CLJ-2499 "Some compiler expr evals report as wrong error phase". The reference page adds: "Other programs may wish to take advantage of this functionality, and it is available in report-error, which takes a Throwable and optionally the :target."

CLJ-2463's description is where the "hide the stack trace by default" decision was made: "The general proposal is to catch Throwable around the call to the main entry point and at least run the Throwable->map, ex-triage, ex-str pipeline. Also need to consider what to do about printing the stack trace. The default JVM behavior is to print the stack of every exception in the chain, but printing either the root cause stack (in execution error) or no stack (in reader/compiler/macroexpansion cases) would likely be better." … "As a macroexpansion spec error, this could just say: … which would be much less scary." … "clojure.main is a process boundary. As such, the facilities for communicating information through it are limited to printing and the return value." Alex Miller's journal (https://insideclojure.org/2019/01/18/journal/, WebFetch): "we did a ton of work to better report errors from the clojure.main repl, but we did not extend that work to clojure.main -m, -e, etc modes!"

### B.4 Clojure 1.10.2 / 1.10.3 / 1.11 / 1.12 / 1.13-alpha
- **1.10.2** added CLJ-2564 "Improve error message for case"; **1.10.3** section "1 Changes reverted" reverts CLJ-2564. The ticket (Alex Miller, 2020-03-13, from ask.clojure.org #9152) is now status **Reopened** with no fix version.
- **1.11.0** (22 Mar 2022; https://clojure.org/news/2022/03/22/clojure-1-11-0 — announcement mentions no error work per WebFetch). changes.md "4.4 Error messages": CLJ-2529 "Fix incorrect reporting of runtime errors as compiler errors in calls through `Compiler.load()`" (Alex Miller; ticket says "the error triage changes in 1.10 are taking care of making better messages now"); CLJ-2350 "Improve keyword arity exception message" (Marc O'Morain patch, created 2018, resolved 2021). Also CLJ-2620 prepl `:exception` handling, CLJ-1908 clojure.test `run-test`.
- **1.12.0** (5 Sep 2024; announcement mentions no error work per WebFetch). changes.md: CLJ-2739 "ArityException - Fix message when function incorrectly called with >20 args"; CLJ-2640 "`ex-info` - now handles nil data map"; CLJ-2726 "`#uuid` data reader - Fix exception on invalid input so it flows through reader"; **1.12.2** CLJ-2914 "Compiler - syntax error if qualified instance method expression is missing instance".
- **1.13.0-alpha1…alpha6** (Jul 2026; https://clojure.org/news/2026/07/02/clojure-1-13-alpha1 … /2026/07/28/clojure-1-13-alpha6, curl-verified). Error-adjacent items: alpha4 **CLJ-2870** "Exception phase during top-level eval is miscategorized" (Alex Miller, created 2024-07-19, resolved 2026-07-14, fix 1.13 — the "Full report" edn wrongly said "Syntax error macroexpanding…" for a script's top-level throw); alpha1 **CLJ-2949** "req! - Variant of get that reports on key not found" and **CLJ-2961** "Checked keys" (both Michael Fogus, 2026) — motivation verbatim: "missing required keys, misspelled keys, and invalid values can lead to failures that do not connect to the actual source of the problem (e.g. NPEs) making diagnosis difficult"; alpha6: "Report as errors any use of unbound/unreferenced keys in :or when using any of the new constructs (:defaults, :select et al)". No 1.13 item touches ex-triage/phases/printing beyond CLJ-2870.

### B.5 Design documents (core-team authored)
- **Design wiki "Better Error Messages"** (Stuart Halloway; child of "Usability" page dated 14 Jun 2013 — https://archive.clojure.org/design-wiki/display/design/Usability.html; page fetched via Wayback https://web.archive.org/web/2018id_/https://dev.clojure.org/display/design/Better+Error+Messages). Verbatim: "This patch improves Clojure's error message for a single common error: passing a non-seq where a seq is neede. More importantly, it is intended as a prototype for other similar improvements in the future." Then: "Idea 1: Error information is data, and type-based error handling should be discouraged. So prefer the ExceptionInfo class for reporting all errors." "Idea 2: Error behavior is typically unspecified, or minimally specified, in Clojure. This is deliberate, and the tests should reflect this." "Idea 4: ExceptionInfo should not serialize data into the error message string. This is a can of worms: What if the data is big? What if serializing the data itself causes further errors? What if the data is lazy?" "Idea 5: People (and tools) may not be accustomed to looking in the map (i.e. calling ex-data) to get details about problems." (This corresponds to CLJ-1099 shipped in 1.6.)
- **Design wiki "Macro Grammars"** (Alex Miller; https://archive.clojure.org/design-wiki/display/design/Macro%2BGrammars.html, fetched via Wayback). Problem: "Validation is usually incomplete, checking only for the most common syntactic errors / Error messages occur inside the macro rather than inside the user's original syntax / Syntax documentation is hand-written and can thus be at odds with the parser". Proposal: "In the case of an error, the parser should automatically produce an error message describing what was provided as input, what the grammar expected, and what was found instead." Lists CLJ-5, CLJ-1473, CLJ-888, CLJ-1029, CLJ-1149 etc. as bugs it would fix and cites Fleming's talk and Czaplicki's "Compiler Errors for Humans". This is the pre-spec plan; the spec rationale (below) explicitly supersedes it.
- **Design wiki "Exception handling update"** (Alex Miller; "Note: Still a WIP"; https://archive.clojure.org/design-wiki/display/design/Exception%2Bhandling%2Bupdate.html — note the `%2B` encoding is required, the `+` URL returns 403). Problems: "Errors can occur in several phases but all get reported in the same way / Can we distinguish between these phases based on where it occurs? / How can we format exceptions for each kind of exception". Body is a table of phases (read at REPL, read in compile, macroexpand-spec, macroexpand-thrown, macroexpand-accidental, compile, evaluation, evaluation/spec instrument, assertion, print) with columns "What is thrown intentionally? / What is thrown accidentally? / Where thrown? / Detect where? / Message template / Example error / Example message / Message in 1.9". Linked from CLJ-2373.
- **Spec rationale** (https://clojure.org/about/spec, curl-verified; Rich Hickey's document): "Many users, especially beginners, are frustrated and challenged by the error messages produced by hand-written parsing and destructuring code, especially in macros where there are two contexts of execution (the macro runs at compile time and its expansion at runtime, either of which could fail due to user error). This has led to a call for 'macro grammars', but in fact macros are just functions of data→data and any solution for data validation and destructuring should work as well for them as for any other functions. I.e. macros are an instance of the problems above."
- **JIRA label statistics** (https://clojure.atlassian.net/rest/api/3/search/jql, `labels in (errormsgs, error-reporting)`, fetched 2026-09-06): 140 tickets; status Closed 78, Open 58, Reopened 4; resolutions Completed 55, Fixed 4, Declined 11, Duplicate 4, Not Reproducible 3, Won't Fix 1, unresolved 62. Closed-with-fix-version by release: 1.10 → 21, 1.9 → 12, 1.7 → 7, 1.6 → 6, 1.8 → 4, 1.10.1 → 4, 1.11 → 2, 1.12 → 2. Created per year: 2013 17, 2014 18, 2015 11, 2016 8, 2017 15, **2018 25**, 2019 7, 2020 3, 2021 3, 2022 8, 2023 1, 2024 3, 2025 1. Oldest still-open: CLJ-5 "Sequential destructuring of map gives confusing error message" (2009). Caveat: labels are applied inconsistently (e.g. CLJ-2456 below has none), so this is a lower bound.

### B.6 ask.clojure.org
Tag pages `/tag/errors`, `/tag/errormsgs`, `/tag/error-reporting` returned HTTP 404 for me, so I could not enumerate by tag. Individual threads verified:
- https://ask.clojure.org/index.php/8343/display-clojure-stacktraces-planned-attacked-clojure-version (Aug 2019, tags stacktrace/roadmap): **alexmiller**: "There are a number of libraries available to alter the printing of stacktraces offering things like collapsing, clojure framing, and colorization. We do not currently have any planned work on this in the next Clojure release."
- https://ask.clojure.org/index.php/4261/clojure-aware-error-stacktrace-rendering (Nikita Prokopov, Dec 2018) → **CLJ-2456, status Open**, no label, no fix version.
- https://ask.clojure.org/index.php/10470/stacktraces-clojure-functions-contain-irrelevant-information (vlaaad, Apr 2021): **alexmiller** (best answer): "I do really want to see the actual stack trace so I don't think that should go away. But it's also useful to think about tools that can be applied to stack traces to give you better insight. That said, a lot of people have poured time into this and the results are not (imo) better than the original. I say that not to throw water on it, but just to note that it seems easy but the details are complicated."
- https://ask.clojure.org/index.php/6566/improve-error-printing-in-clojure-main-with-m-e-etc → CLJS-3044 (the ClojureScript counterpart of CLJ-2463).
- https://ask.clojure.org/index.php/9152/improve-error-messages-for-clojure-core-case → CLJ-2564 (shipped 1.10.2, reverted 1.10.3, Reopened).
- CLJ-2501 "Provide guidance on configuring error printer for handling errors" (Ben Brinckerhoff/Expound, 2019) — Open.

### B.7 Talks
- I found **no dedicated conference talk** by Alex Miller or Stuart Halloway about the 1.10 error work. What exists: Strange Loop 2018 unsession (https://www.thestrangeloop.com/2018/unsession-clojure-meetup.html, curl-verified): "Alex Miller and Stuart Halloway of the Clojure core team will give an update on Clojure 1.10 and answer questions." Clojure/conj 2018 speaker list (http://2018.clojure-conj.org/speakers/) shows Rich Hickey ("Maybe Not") and Stuart Halloway (title not captured; Sean Corfield's preview https://corfield.org/blog/2018/10/25/clojure-conj/ says only "he hinted that this talk would be about new tooling he has been using. Maybe prepl-related?") and no Alex Miller talk. The only error-message talk at conj 2018 was the community "Babel" talk.
- Stuart Halloway, "REPL-Driven Development" (June 2017, https://vimeo.com/223309989; transcript cloned) — see C.2.
- Stuart Halloway, "Debugging with the Scientific Method" (Clojure/conj 2015, https://www.youtube.com/watch?v=FihU5JxmnBg; transcript cloned) — see C.2.
- Rich Hickey, "clojure.spec" (LispNYC, Nov 2016, https://vimeo.com/195711510; transcript cloned) — see C.2.

---

## C. The core team's stated philosophy / position

### C.1 The 1.10 principles (CLJ-2373, Stuart Halloway, 9 Jul 2018) — verbatim
> **Problems**
> * Discussions about "make errors better" often conflate error message, stacktraces, exception data
> * Errors do not clearly indicate in what "phase" of execution they occur (read, compile, macroexpand, evaluation, printing)
> * Errors during macroexpansion do not capture the location in caller source (like read and compile exceptions do), and thus the wrong "location" is reported
> * Clojure prints data into Throwable messages — big and ugly — outside user control
>
> **Principles**
> * exception messages should be small strings — with the expectation that getMessage will be printed — totally controlled by throwing code — never print arbitrary data into a message
> * flow data all the way to the edge — ex-info & ex-data — macroexpand spec errors have a well known format
> * edge printing should be concise by default, configurable by user — edge functions like repl-caught respect print and spec bindings — concise summary by default — all the details when you want
>
> **Discussion**
> * what should tools do? — leverage the data provided in the exception chain to do whatever they want — can use main/repl-caught as a guide
> * what happens to programs that parse exception messages? — probably should not do this (Clojure's tests do this but try to minimize it) — this proposal changes some wrapper message text, but that was never part of the contract

### C.2 Other statements, by person

**Stuart Halloway, clojure mailing list, 8 Feb 2011, "better error messages > smaller stack traces"** (https://groups.google.com/d/topic/clojure/WxV5Oego4R0, WebFetch, quoted as verbatim by the tool):
> The core team is very interested in improving error messages, and relatively less interested in more code to manipulate stack traces, for the following reasons: (1) The language sits at the bottom, and must solve the problems nobody higher up can solve. Error messages are created at the point of an error, and if the language gets it wrong you may not have the information to fix it later. OTOH, any old library code can reduce a stacktrace. (2) Better error messages are easy to implement. Most oddball errors are in macroexpansion, so there is no runtime cost to doing extra work to improve error messages. These can be implemented as pure functions, so even a relative beginner could contribute a patch. (3) The clojure.repl/pst macro in 1.3 already provides better control of stack trace spewage at the REPL. Please let us know when you get a misleading error message from a macroexpansion, so we can make it better. Or contribute a patch along the lines of [2].

**Stuart Halloway, "Debugging with the Scientific Method", Clojure/conj 2015** (transcript): after showing a `ClassCastException … cannot cast java.lang.String to clojure.lang.IFn`: "We could make error messages better … we could anticipate every possible scenario everybody could ever get into. And, not to be unfair to error messages, we could go a long way towards having better error messages without being ridiculously over the top about it. Error message could be made better: sounds like a good plan to be." He opens the talk with a Reddit thread that "captures … the sort of beginner experience of Clojure; people, when they are new to Clojure, particularly struggle with debugging problems".

**Stuart Halloway, "REPL-Driven Development", June 2017** (transcript, 13 months before he filed CLJ-2373):
> Given that we have this technology at the REPL, what is the right answer for making the Clojure beginner experience better in terms of error messages? Let me ask a slightly different question. Given that the REPL works this way, does Clojure need to incorporate a bunch of changes in its 1.9 release cycle in order to provide a better experience for beginners? No. Who can make a better experience for beginners? Anybody! Anybody can. If you have a particular error message that just absolutely chaps your butt, then run under a REPL that does something different when that error happens.

Same period, Cognitect blog "REPL Debugging: No Stacktrace Required" (https://www.cognitect.com/blog/2017/6/5/repl-debugging-no-stacktrace-required, 6 Jun 2017, WebFetch): "I am going to deliberately obfuscate Clojure's error reporting. I am going to throw away all the potentially useful information: the exception classes, the stacktraces, and the error messages." — arguing the REPL, not the message, is the debugging tool.

**Rich Hickey.** Spec rationale quote (B.5). LispNYC clojure.spec talk, Nov 2016 (transcript):
> Some of the things that it is responding to are, the community wanted to say, "Oh, we want to get better errors from macros." And I will talk about that in a second. It ends up that that problem is not a different problem than specifying how data structures should be assembled. If you have error reporting and validation for data structures, you have error reporting and validation for macros.

> …which is why error reporting for macros turns into this problem, because that is all that macros do.

> So we are going to do that in pieces. We are starting with the macros, because that was the thing that people wanted better error reporting around. But yeah, there will be specs for a lot of the language functions.

(For completeness: Clojure/conj 2012 "The Language of the System", https://www.youtube.com/watch?v=ROor6_NGIWU, transcript: "All right, of course this is my favorite topic: errors and error messages and whatever. [Audience laughter]" — a self-aware joke in a section about partial failure in distributed systems, not a policy statement.) I found no Rich Hickey statement that the stack trace "is for Java interop" or similar; the "errors should be data" idea is Halloway's (2013 wiki, CLJ-2373).

**Alex Miller.** 1.10 write-up quotes (B.2); ask.clojure.org quotes (B.6): no planned stack-trace work (2019); wants the real stack trace kept and thinks prettifiers have not beaten the original (2021). CLJ-2463: "we have expended a lot of energy to craft a good message for all phases in the 1.10 error message re-work". Survey 2018: "The top result is clearly error messages though - while spec has started us down a road, that is still a work in progress which will continue this year."

**Community characterizations of the core position** (not core statements): Eric Normand (WebFetch) describes Cognitect's historical response to error-message complaints as "alienating" and argues the errors are "accidental" because "Clojure's core functions are, for the most part, implemented without checks on the arguments." A 2015 respondent (Daniel Compton) describes the "patches welcome" treatment.

### C.3 The FAQ
https://clojure.org/guides/faq (curl-verified) has **no entry on error messages, exceptions or stack traces**; the only occurrence of "error" is in the transducers entry. It does have, under Contributing: "Other projects hosted on GitHub accept pull requests. Why not Clojure? — Rich Hickey prefers to evaluate patches attached to JIRA tickets. This is not to make it more difficult for contributors, or for legal reasons, but because of workflow preferences."

### C.4 Governance — why an outsider cannot "just fix this"
- https://clojure.org/dev/dev (curl-verified): "Clojure was created by Rich Hickey and is developed by a core team of developers at Nubank, which supports this work. The Clojure development team values a measured and thoughtful approach to language evolution with a strong emphasis on maintaining backward compatibility." / "Clojure's direction is determined by Rich Hickey and the core team. This process is open to input and visible in issues and commits, but not explicitly driven by the community. The core team pays attention to the needs of the community by monitoring and participating in many Clojure discussion forums, and by reviewing issues and votes in the issue tracker." / "Clojure accepts contributions as patches on issues. … Tickets may be assessed over a long period of time, following the workflow." / "enhancement/feature tickets should start with a compelling problem to solve and compare alternatives and their tradeoffs, rather than leap straight to a solution and a patch. Clojure is a small language and endeavors to remain so. In many cases, proposed features can instead be provided in functions or libraries outside the core." / "The core team tends to focus on tickets primarily in the late alpha / early beta period for a release cycle. During other parts of the release cycle, activity may seem dormant".
- https://clojure.org/dev/workflow: "BDFL - Rich Hickey is the creator and Benevolent Dictator for Life of what goes into Clojure. Stuart Halloway also has a special level of access and typically commits patches to Clojure." Stages: Triaged → Prescreened → Vetted ("screener and Rich have approved the ticket as worth working on") → Screened ("screener has approved a ticket's patch for review by Rich") → Ok ("Rich has approved the ticket for inclusion").
- https://clojure.org/dev/contributor_agreement: "the Clojure project requires contributors to jointly assign their copyright on contributed code. The Contributor Agreement (CA) gives Rich Hickey and the contributor joint copyright interests in the code" / "In order for the CA to be effective, the Clojure project must obtain an assignment for all contributions." Also (on this page and on developing_patches and community/contributing): "Clojure's code is written and reviewed by humans. Code generated by a large language model or similar technology, such as Anthropic's Claude, GitHub/Microsoft's Copilot, OpenAI's ChatGPT, Facebook/Meta's Code Llama et al, is not compliant with the covenants and representations of Clojure's Contributor's Agreement, and is thus not acceptable as code for Clojure."
- https://clojure.org/community/contributing: "The Clojure team provides a forum where users can ask questions, submit potential problems, and request enhancements … Mark the question with tag problem for potential problems and request for enhancements. The community and core team will assess the issue and determine whether to file an issue in the jira tracker." / "The development teams for these languages and libs will use the question votes to prioritize their work in jira towards the next release."
- https://clojure.org/dev/creating_tickets (WebFetch): "To create a ticket, you must already have a Clojure JIRA account." Otherwise "please follow the preferred process for filing an issue at ask.clojure.org."
- https://clojure.org/dev/developing_patches: "Don't do too much! Submit small patches that address specific problems, don't add anything extra, even (especially!) "cleanup" of nearby code."

Concrete illustration of the pipeline's latency: CLJ-2350 (keyword arity message, community patch) was filed 2018-05-03 and shipped in 1.11 on 2022-03-22; CLJ-2870 (phase miscategorized, filed by Alex Miller himself 2024-07-19) shipped in 1.13-alpha4 in July 2026; CLJ-5 (2009) is still Open.

---

## Timeline

| Year | Version / event | What changed | Source |
|---|---|---|---|
| 2011-02 | Mailing list | Halloway: "core team is very interested in improving error messages, and relatively less interested in more code to manipulate stack traces" | https://groups.google.com/d/topic/clojure/WxV5Oego4R0 |
| 2013 | 1.5 / design wiki | 1.5 "Improved error messages" section; Halloway's "Better Error Messages" page: "Error information is data"; "ExceptionInfo should not serialize data into the error message string" | changes.md; Wayback of dev.clojure.org/display/design/Better+Error+Messages |
| 2014-03 | 1.6 | "3.2 Error messages": CLJ-1099 seq errors as ExceptionInfo w/ ex-data, etc. | changes.md |
| 2015-06 | 1.7 | `Throwable->map`, `#error` data printing; "3.1 Error messages" | changes.md |
| 2015-11 | Clojure/conj | Fleming "Improving Clojure's Error Messages with Grammars"; Halloway "Debugging with the Scientific Method" | youtube kt4haSH2xcs; transcript |
| ~2015 | design wiki | Miller's "Macro Grammars" proposal (pre-spec) | archive.clojure.org/design-wiki/…/Macro%2BGrammars.html |
| 2016-01 | State of Clojure 2015 | "Error messages" added as a choice; immediately #1 frustration; "We expect to evaluate possible changes in this area" | cognitect.com/blog/2016/1/28/… |
| 2016-05/11 | spec announced; rationale | "Many users, especially beginners, are frustrated and challenged by the error messages produced by hand-written parsing…" | clojure.org/about/spec; LispNYC transcript |
| 2017-01 | State of Clojure 2016 | Error messages #1 again; "we will continue to improve those with the integration of spec in Clojure 1.9" | clojure.org/news/2017/01/31/… |
| 2017-06 | Halloway talks | "does Clojure need to incorporate a bunch of changes in its 1.9 release cycle…? No. … Anybody can." | REPLDrivenDevelopment transcript; Cognitect blog |
| 2017-12 | 1.9 | Specs checked on core macros at compile time; "did not conform to spec" messages with explain data in message | changes.md §2.1; clojure19 post |
| 2018-03 | State of Clojure 2018 | "The top result is clearly error messages though - while spec has started us down a road, that is still a work in progress" | clojure.org/news/2018/03/19/… |
| 2018-03 | Community | Compton: "this year they have gotten worse with the introduction of specs on core macros"; Normand "…Accidental" (June); ClojureVerse thread | danielcompton.net; ericnormand.me; clojureverse |
| 2018-07-09 | CLJ-2373 filed | Halloway's problems/principles; design wiki "Exception handling update" (Miller) | JIRA REST; archive.clojure.org |
| 2018-09 | Strange Loop unsession | "Alex Miller and Stuart Halloway … will give an update on Clojure 1.10" | thestrangeloop.com |
| 2018-10/12 | CLJ-2415/2420/2430/2435/2438 | Phase names finalized, ex-triage split from ex-str, phase elevated in Throwable->map, cause class, demunging | JIRA REST |
| 2018-11 | Clojure/conj 2018 | Machkasova et al. "Babel: middleware for beginner-friendly error messages" (community) | 2018.clojure-conj.org/speakers/ |
| 2018-12-17 | 1.10 | Phases, :clojure.error/* ex-data, ex-triage/ex-str, repl-caught rewritten | changes.md; clojure110 post; insideclojure.org/2018/12/17/errors/ |
| 2019-01 | CLJ-2463 filed | -m/-e/script modes still dump JVM traces; "would be much less scary" | JIRA REST; insideclojure journal 2019.3 |
| 2019-02 | State of Clojure 2019 | Official post silent on frustrations; Compton: complaints continue, some thanks for 1.10 | clojure.org; danielcompton.net |
| 2019-06-06 | 1.10.1 | Launcher uses triage; full trace to temp file by default; `--report`/`-Dclojure.main.report=file|stderr|none`; `report-error` | changes.md §1.2; reference "As launcher" |
| 2019-08 | ask.clojure.org | Miller: "We do not currently have any planned work on this in the next Clojure release." | ask.clojure.org/index.php/8343 |
| 2020/2021 | 1.10.2 / 1.10.3 | CLJ-2564 `case` message improved then reverted (ticket Reopened) | changes.md |
| 2021-04 | ask.clojure.org | Miller: "I do really want to see the actual stack trace … it seems easy but the details are complicated." | ask.clojure.org/index.php/10470 |
| 2022-03-22 | 1.11 | "4.4 Error messages": CLJ-2529 phase fix in Compiler.load(); CLJ-2350 keyword arity | changes.md |
| 2023-06 | State of Clojure 2023 | Q7 open responses published; ~8% mention errors/stacktraces (my count) | download.clojure.org/…/Data_Q7_230630.pdf |
| 2024-09-05 | 1.12 | CLJ-2739 arity message, CLJ-2640 ex-info nil map; 1.12.2 CLJ-2914 | changes.md |
| 2024-12 | State of Clojure 2024 | New users: "Which areas would you prioritize for Clojure improvements?" → Error messages 48.62% (#1) | clojure.org/news/2024/12/02/… (new-priorities.png) |
| 2025-02/03 | Community | HN: "decade of surveys showing that error messages/stacktraces were people's top complaints"; jank blog | news.ycombinator.com/item?id=43203265; jank-lang.org |
| 2026-02 | State of Clojure 2025 | No error-message content in post | clojure.org/news/2026/02/18/… |
| 2026-07 | 1.13.0-alpha1…6 | CLJ-2870 phase miscategorization fixed (alpha4); checked keys / `req!` (Fogus) | clojure.org/news/2026/07/… ; JIRA |

---

## Open questions I could not verify

1. **Survey questionnaires 2019–2023 and 2025.** The official posts for those years say nothing about a frustration/priority question. The full SurveyMonkey result pages (e.g. https://www.surveymonkey.com/results/SM-S9JVNXNQV/, https://www.surveymonkey.com/results/SM-5QgPUw4aBbMUY3FJVRra2w_3D_3D/) are JavaScript apps that returned only a header to my fetches, so I cannot say whether "error messages" was asked/ranked in those years or how it ranked for the *whole* community in 2024 (the post implies a whole-community "prioritize" chart exists — "These don't vary too much from the community as a whole" — but only shows the new-user one). Therefore the jank blog's "consistently at the top … all the way back to 2015" is verified for 2015, 2016, 2018 and 2024-new-users only.
2. **Exact percentages** for "Error messages" in the 2015 and 2016 charts (images) were not read.
3. **Stuart Halloway's Clojure/conj 2018 talk** title/content (speaker page returned HTTP 406). Whether either he or Alex Miller ever gave a *dedicated* talk on the 1.10 error work: I found none; absence is not proof.
4. **Design wiki "Error Handling" and "Platform Errors" pages** returned 403 from both archive.clojure.org and Wayback; not read.
5. **Stuart Halloway's 2018 tweets** on error messages (referenced by Eric Normand's newsletter) were not fetched.
6. The Google Groups 2011 thread text came through the WebFetch summarizer (which labelled it verbatim); I could not view the raw page.
7. The 2023 Q7 counts are naive substring matches on 971 responses; no manual coding was done.
8. CLJ-2373's attachment "example-errors.txt" and the 1.10 alpha-by-alpha evolution were not examined.
9. I did not fetch Reddit threads (search returned none directly); the Reddit thread Halloway referenced in 2015 was not identified.
10. Whether the core team currently (2025–2026) has any stated roadmap item on error messages: nothing found in 1.12/1.13 release notes, ask.clojure.org answers, or the 2024/2025 survey posts; the most recent explicit statement found is Alex Miller's 2019 "no planned work" (about stack-trace printing) and his 2021 answer.
