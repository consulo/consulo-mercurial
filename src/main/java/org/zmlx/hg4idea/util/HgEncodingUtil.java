package org.zmlx.hg4idea.util;

import consulo.project.Project;
import consulo.virtualFileSystem.encoding.EncodingProjectManager;

import jakarta.annotation.Nonnull;

import java.nio.charset.Charset;

import static org.zmlx.hg4idea.HgVcs.HGENCODING;

/**
 * @author Kirill Likhodedov
 */
public class HgEncodingUtil {

  @Nonnull
  public static Charset getDefaultCharset(@Nonnull Project project) {
    if (HGENCODING != null && HGENCODING.length() > 0 && Charset.isSupported(HGENCODING)) {
      return Charset.forName(HGENCODING);
    }
    Charset defaultCharset = null;
    if (!project.isDisposed()) {
      defaultCharset = EncodingProjectManager.getInstance(project).getDefaultCharset();
    }
    return defaultCharset != null ? defaultCharset : Charset.defaultCharset();
  }

  @Nonnull
  public static String getNameFor(@Nonnull Charset charset) {
    //workaround for x_MacRoman encoding etc; todo: create map with encoding aliases because some encodings name are not supported by hg
    String name = charset.name();
    if (name.startsWith("x-M")) {
      return name.substring(2); // without "x-" prefix;
    }
    return name;
  }
}
