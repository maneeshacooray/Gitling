(function() {
  var lang = void 0;

  var editor = void 0;

  var displayFileContent = function() {
    var rawCodes = CodeLoader.getCode();
    $('#editor').text(rawCodes);
    var editorElm = document.getElementById("editor");
    var editorOption = {
      lineNumbers: true,
      mode: lang,
      theme: CodeLoader.getTheme(),
      matchBrackets: true,
      lineWrapping: true,
      readOnly: true
    };
    editor = CodeMirror.fromTextArea(editorElm, editorOption);
    // CSS height:100%/100vh chains don't resolve in this WebView (html/body computed height
    // comes back 0px even though window.innerHeight is correct) -- set pixel heights directly
    // from JS instead, which sidesteps that entirely. Re-applied on resize since this Activity
    // survives rotation (configChanges) and the WebView is resized in place, not reloaded.
    var syncSize = function() {
      document.body.style.height = window.innerHeight + "px";
      if (editor) editor.setSize("100%", window.innerHeight + "px");
    };
    syncSize();
    window.addEventListener('resize', syncSize);
  };

  window.setLang = function(l) {
    lang = l;
    if (editor) {
      return editor.setOption("mode", lang);
    }
  };

  window.display = displayFileContent;

  window.setEditable = function() {
    editor.setOption("readOnly", false);
  };

  window.save = function() {
    editor.setOption("readOnly", true);
    CodeLoader.save(editor.getValue());
  };

  window.copy_all = function() {
    value = editor.getValue();
    CodeLoader.copy_all(value);
  }


  $(document).ready(function() {
    CodeLoader.loadCode();
  });

}).call(this);
