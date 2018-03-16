package org.apache.xml.utils.res;

import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * <meta name="usage" content="internal"/>
 * The default (english) resource bundle.
 */
public class XResourceBundle extends ListResourceBundle
{

  /** Error resource constants */
  public static final String ERROR_RESOURCES =
    "org.apache.xalan.res.XSLTErrorResources", XSLT_RESOURCE =
    "org.apache.xml.utils.res.XResourceBundle", LANG_BUNDLE_NAME =
    "org.apache.xml.utils.res.XResources", MULT_ORDER =
    "multiplierOrder", MULT_PRECEDES = "precedes", MULT_FOLLOWS =
    "follows", LANG_ORIENTATION = "orientation", LANG_RIGHTTOLEFT =
    "rightToLeft", LANG_LEFTTORIGHT = "leftToRight", LANG_NUMBERING =
    "numbering", LANG_ADDITIVE = "additive", LANG_MULT_ADD =
    "multiplicative-additive", LANG_MULTIPLIER =
    "multiplier", LANG_MULTIPLIER_CHAR =
    "multiplierChar", LANG_NUMBERGROUPS = "numberGroups", LANG_NUM_TABLES =
    "tables", LANG_ALPHABET = "alphabet", LANG_TRAD_ALPHABET = "tradAlphabet";

  /**
   * Return a named ResourceBundle for a particular locale.  This method mimics the behavior
   * of ResourceBundle.getBundle().
   *
   * @param className Name of local-specific subclass.
   * @param locale the locale to prefer when searching for the bundle
   */
  public static final XResourceBundle loadResourceBundle(
          String className, Locale locale) throws MissingResourceException
  {

    String suffix = getResourceSuffix(locale);

    try
    {
      
      String resourceName = className + suffix;
      return (XResourceBundle) ResourceBundle.getBundle(resourceName, locale);
    }
    catch (MissingResourceException e)
    {
      {

        return (XResourceBundle) ResourceBundle.getBundle(
          XSLT_RESOURCE, new Locale("en", "US"));
      }
      catch (MissingResourceException e2)
      {

        throw new MissingResourceException(
          "Could not load any resource bundles.", className, "");
      }
    }
  }

  /**
   * Return the resource file suffic for the indicated locale
   * For most locales, this will be based the language code.  However
   * for Chinese, we do distinguish between Taiwan and PRC
   *
   * @param locale the locale
   * @return an String suffix which canbe appended to a resource name
   */
  private static final String getResourceSuffix(Locale locale)
  {

    String lang = locale.getLanguage();
    String country = locale.getCountry();
    String variant = locale.getVariant();
    String suffix = "_" + locale.getLanguage();

    if (lang.equals("zh"))
      suffix += "_" + country;

    if (country.equals("JP"))
      suffix += "_" + country + "_" + variant;

    return suffix;
  }

  /**
   * Get the association list.
   *
   * @return The association list.
   */
  public Object[][] getContents()
  {
    return contents;
  }

  /** The association list. */
  static final Object[][] contents =
  {
    { "ui_language", "en" }, { "help_language", "en" }, { "language", "en" },
    { "alphabet",
      new char[]{ 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
                  'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
                  'Y', 'Z' } },
    { "tradAlphabet",
      new char[]{ 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
                  'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
                  'Y', 'Z' } },

    { "orientation", "LeftToRight" },

    { "numbering", "additive" },
  };
}