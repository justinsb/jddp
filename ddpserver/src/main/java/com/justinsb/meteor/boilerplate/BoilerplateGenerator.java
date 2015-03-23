package com.justinsb.meteor.boilerplate;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.gson.JsonObject;

public abstract class BoilerplateGenerator {
  final Manifest manifest;

  final List<ManifestItemData> css = Lists.newArrayList();
  final List<ManifestItemData> js = Lists.newArrayList();
  ManifestItemData head;
  ManifestItemData body;

  final File baseDir;

  final JsonObject meteorRuntimeConfig;

  public BoilerplateGenerator(Manifest manifest, File baseDir, JsonObject meteorRuntimeConfig) throws IOException {
    this.manifest = manifest;
    this.baseDir = baseDir;
    this.meteorRuntimeConfig = meteorRuntimeConfig;
    parseManifest(true);
  }

  public static class ManifestItemData {
    public String url;
    public String content;
    public boolean inline;
  }

  private void parseManifest(boolean inline) throws IOException {
    for (Manifest.ManifestEntry item : manifest.entries) {
      String urlPath = item.url; // urlMapper(item.url);

      ManifestItemData itemObj = new ManifestItemData();
      itemObj.url = urlPath;

      itemObj.content = Files.toString(new File(baseDir, item.path), Charsets.UTF_8);

      if (inline) {
        itemObj.inline = true;
      }

      if ("css".equals(item.type) && "client".equals(item.where)) {
        css.add(itemObj);
      }
      if ("js".equals(item.type) && "client".equals(item.where)) {
        js.add(itemObj);
      }

      if ("head".equals(item.type)) {
        head = itemObj;
      }

      if ("body".equals(item.type)) {
        body = itemObj;
      }
    }
  }

  public abstract void writeHtml(Writer writer) throws IOException;

  protected boolean isInlineScriptsAllowed() {
    return true;
  }

  protected List<ManifestItemData> getCss() {
    return css;
  }

  protected List<ManifestItemData> getJs() {
    return js;
  }

  protected void writeHead(Writer writer) throws IOException {
    if (head != null) {
      writer.write(head.content);
    }
  }

  protected void writeBody(Writer writer) throws IOException {
    if (body != null) {
      writer.write(body.content);
    }
  }

  protected String getHtmlAttributes() {
    return "";
  }

}
