package com.justinsb.meteor;

import java.io.File;
import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;
import com.justinsb.meteor.boilerplate.BrowserBoilerplateGenerator;
import com.justinsb.meteor.boilerplate.Manifest;

public class BoilerplateFilter implements Filter {
  private static final long serialVersionUID = 1L;

  final File baseDir;
  final JsonObject meteorRuntimeConfig;
  final String bundledJsCssPrefix;

  public BoilerplateFilter(File baseDir, JsonObject meteorRuntimeConfig, String bundledJsCssPrefix) {
    super();
    this.baseDir = baseDir;
    this.meteorRuntimeConfig = meteorRuntimeConfig;
    this.bundledJsCssPrefix = bundledJsCssPrefix;
  }

  @Override
  public void destroy() {

  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain next) throws IOException, ServletException {
    if (req instanceof HttpServletRequest) {
      HttpServletRequest request = (HttpServletRequest) req;
      String path = request.getContextPath();
      if ("".equals(path)) {
        serveIndex(request, (HttpServletResponse) resp);
        return;
      }
    }
    next.doFilter(req, resp);
  }

  private void serveIndex(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    Manifest manifest = Manifest.parse(new File(baseDir, "program.json"));
    BrowserBoilerplateGenerator generator = new BrowserBoilerplateGenerator(manifest, baseDir, meteorRuntimeConfig,
        bundledJsCssPrefix);
    resp.setStatus(200);
    resp.addHeader("content-type", "text/html; charset=utf-8");
    generator.writeHtml(resp.getWriter());
  }

  @Override
  public void init(FilterConfig arg0) throws ServletException {

  }

}
