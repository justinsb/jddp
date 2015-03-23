package com.justinsb.meteor.boilerplate;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;

public class Manifest {
  final List<ManifestEntry> entries;

  private Manifest(List<ManifestEntry> entries) {
    this.entries = entries;
  }

  public static Manifest parse(File path) throws IOException {
    String jsonString = Files.toString(path, Charsets.UTF_8);

    JsonRepresentation json = new Gson().fromJson(jsonString, JsonRepresentation.class);
    return new Manifest(json.manifest);
  }

  static class JsonRepresentation {
    public String format;
    public List<ManifestEntry> manifest;
  }

  public static class ManifestEntry {
    public String path;
    public String where;
    public String type;
    public boolean cacheable;
    public String url;
    public long size;
    public String hash;
  }

}
