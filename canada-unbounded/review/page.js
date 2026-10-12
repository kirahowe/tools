(function () {
  var fab = document.querySelector(".comment-fab");
  var tip = document.querySelector(".comment-tip");
  var BLOCKS = "p, li, figure, .table-scroll, h2, h3, blockquote, .callout, .cell-output-display";
  var comments = null;
  var picking = false;
  var tipTimer = null;

  function say(text, ms) {
    tip.textContent = text;
    tip.hidden = !text;
    clearTimeout(tipTimer);
    if (text && ms) tipTimer = setTimeout(function () { tip.hidden = true; }, ms);
  }
  function setPicking(on) {
    picking = on;
    document.body.classList.toggle("picking", on);
    fab.setAttribute("aria-pressed", on ? "true" : "false");
    fab.textContent = on ? "Cancel" : "Comment";
    say(on ? "Tap the paragraph, chart or table you want to comment on." : "", 0);
  }
  function selectedRange() {
    var sel = window.getSelection && window.getSelection();
    if (!sel || sel.isCollapsed || !sel.rangeCount) return null;
    var r = sel.getRangeAt(0);
    var post = document.querySelector(".post");
    return post && post.contains(r.commonAncestorContainer) ? r : null;
  }
  function open(target) {
    comments.openComposer(target).then(function (res) {
      if (!res.opened) say("Finish or close the comment you already have open first.", 3500);
    }).catch(function (e) {
      if (e && e.code === "unavailable") { fab.hidden = true; say("Commenting isn't available in this view.", 4000); }
      else if (e && e.code === "rate_limited") say("Give it a moment, then try again.", 3000);
    });
  }

  fab.addEventListener("click", function () {
    if (!comments) return;
    var r = selectedRange();
    if (r) { setPicking(false); open({ range: r }); return; }
    setPicking(!picking);
  });
  // Keep a text selection alive when the button is tapped on touch screens.
  fab.addEventListener("pointerdown", function (e) { e.preventDefault(); });

  document.addEventListener("click", function (e) {
    if (!picking || !comments) return;
    if (e.target.closest("[data-uncommentable]")) return;
    var block = e.target.closest(BLOCKS);
    if (!block || !block.closest(".post, .notes")) return;
    e.preventDefault();
    e.stopPropagation();
    setPicking(false);
    open({ element: block });
  }, true);

  document.addEventListener("keydown", function (e) { if (e.key === "Escape" && picking) setPicking(false); });

  if (window.claude && window.claude.use) {
    window.claude.use("comments").then(function (c) {
      if (!c) return;
      comments = c;
      fab.hidden = false;
    }).catch(function () {});
  }
})();
