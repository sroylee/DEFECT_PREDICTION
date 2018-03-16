package org.apache.lucene.search;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;

/**
 * Wrapper used by {@link HitIterator} to provide a lazily loaded hit
 * from {@link Hits}.
 *
 * @author Jeremy Rayner
 */
public class Hit implements java.io.Serializable {

  private Document doc = null;

  private boolean resolved = false;

  private Hits hits = null;
  private int hitNumber;

  /**
   * Constructed from {@link HitIterator}
   * @param hits Hits returned from a search
   * @param hitNumber Hit index in Hits
   */
  Hit(Hits hits, int hitNumber) {
    this.hits = hits;
    this.hitNumber = hitNumber;
  }

  /**
   * Returns document for this hit.
   *
   * @see Hits#doc(int)
   * @throws CorruptIndexException if the index is corrupt
   * @throws IOException if there is a low-level IO error
   */
  public Document getDocument() throws CorruptIndexException, IOException {
    if (!resolved) fetchTheHit();
    return doc;
  }

  /**
   * Returns score for this hit.
   *
   * @see Hits#score(int)
   */
  public float getScore() throws IOException {
    return hits.score(hitNumber);
  }

  /**
   * Returns id for this hit.
   *
   * @see Hits#id(int)
   */
  public int getId() throws IOException {
    return hits.id(hitNumber);
  }

  private void fetchTheHit() throws CorruptIndexException, IOException {
    doc = hits.doc(hitNumber);
    resolved = true;
  }


  /**
   * Returns the boost factor for this hit on any field of the underlying document.
   *
   * @see Document#getBoost()
   * @throws CorruptIndexException if the index is corrupt
   * @throws IOException if there is a low-level IO error
   */
  public float getBoost() throws CorruptIndexException, IOException {
    return getDocument().getBoost();
  }

  /**
   * Returns the string value of the field with the given name if any exist in
   * this document, or null.  If multiple fields exist with this name, this
   * method returns the first value added. If only binary fields with this name
   * exist, returns null.
   *
   * @see Document#get(String)
   * @throws CorruptIndexException if the index is corrupt
   * @throws IOException if there is a low-level IO error
   */
  public String get(String name) throws CorruptIndexException, IOException {
    return getDocument().get(name);
  }

  /**
   * Prints the parameters to be used to discover the promised result.
   */
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("Hit<");
    buffer.append(hits.toString());
    buffer.append(" [");
    buffer.append(hitNumber);
    buffer.append("] ");
    if (resolved) {
        buffer.append("resolved");
    } else {
        buffer.append("unresolved");
    }
    buffer.append(">");
    return buffer.toString();
  }


}