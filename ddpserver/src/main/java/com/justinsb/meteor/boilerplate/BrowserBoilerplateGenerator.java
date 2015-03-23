package com.justinsb.meteor.boilerplate;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URLEncoder;

import com.google.gson.JsonObject;

public class BrowserBoilerplateGenerator extends BoilerplateGenerator {

  final String bundledJsCssPrefix;

  public BrowserBoilerplateGenerator(Manifest manifest, File baseDir, JsonObject meteorRuntimeConfig,
      String bundledJsCssPrefix) throws IOException {
    super(manifest, baseDir, meteorRuntimeConfig);
    this.bundledJsCssPrefix = bundledJsCssPrefix;
  }

  @Override
  public void writeHtml(Writer writer) throws IOException {
    writer.write("<!DOCTYPE html>\n");

    writer.write("<html " + getHtmlAttributes() + ">");
    writer.write("<head>");
    for (ManifestItemData css : getCss()) {
      String url = bundledJsCssPrefix + css.url;
      writer.write("<link rel='stylesheet' type='text/css' class='__meteor-css__' href='" + url + "'>");
    }

    if (isInlineScriptsAllowed()) {
      String config = "'" + URLEncoder.encode(meteorRuntimeConfig.toString()) + "'";
      writer.write("<script type='text/javascript'>__meteor_runtime_config__ = JSON.parse(decodeURIComponent(" + config
          + "));</script>");
    } else {
      throw new IllegalStateException();
      // String meteorRuntimeConfigUrl = {{rootUrlPathPrefix}}/meteor_runtime_config.js;
      // writer.write("<script type='text/javascript' src='" + meteorRuntimeConfigUrl + "'></script>");
    }

    for (ManifestItemData js : getJs()) {
      String url = bundledJsCssPrefix + js.url;
      writer.write("<script type='text/javascript' src='" + url + "'></script>");
    }

    // for (ManifestItemData js : getAdditionalStaticJs()) {
    // if (isInlineScriptsAllowed()) {
    // writer.write("<script type='text/javascript'>");
    // writer.write(js.getScript());
    // writer.write("</script>");
    // } else {
    // throw new IllegalStateException();
    // // <script type='text/javascript'
    // // src='{{rootUrlPathPrefix}}{{pathname}}'></script>
    // }
    // }

    writeHead(writer);

    writer.write("</head>");
    writer.write("<body>");
    writeBody(writer);
    writer.write("</body>");
    writer.write("</html>");
  }

}