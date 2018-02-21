package org.swellrt.beta.client.platform.web.editor;

import org.swellrt.beta.client.platform.web.editor.history.DocHistoryRemote;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SText;
import org.swellrt.beta.model.SVisitor;
import org.swellrt.beta.model.wave.SubstrateId;
import org.swellrt.beta.model.wave.mutable.SWaveNodeManager;
import org.swellrt.beta.model.wave.mutable.SWaveText;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.playback.DocHistory;
import org.waveprotocol.wave.client.editor.playback.DocHistory.Iterator;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.Blip;

/**
 * A text document supported by a remote wave document.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class STextWebRemote extends SWaveText implements STextWeb {

  private final InteractiveDocument interactiveDoc;
  private DocHistory docHistory;

  public STextWebRemote(SWaveNodeManager nodeManager, SubstrateId substrateId, Blip blip,
      DocInitialization docInit, InteractiveDocument interactiveDoc) {
    super(nodeManager, substrateId, blip);

    Preconditions.checkArgument(interactiveDoc != null,
        "STextWebRemote object requires a InteractiveDocument");

    if (docInit != null && interactiveDoc != null) {
      interactiveDoc.getDocument().consume(docInit);
    }

    this.interactiveDoc = interactiveDoc;
    this.updateHistory();
  }

  protected void updateHistory() {
    this.docHistory = new DocHistoryRemote(getNodeManager().getWaveId(),
        getSubstrateId().getContainerId(), getSubstrateId().getDocumentId(),
        blip.getWavelet().getHashedVersion());

  }

  @Override
  public SMap getLiveCarets() {
    return getNodeManager().getTransient().getCaretsForDocument(getSubstrateId().getDocumentId());
  }

  @Override
  public Iterator getHistoryIterator() {
    updateHistory();
    return docHistory.getIterator();
  }

  @Override
  public SNode node(String path) throws SException {
    return null;
  }

  @Override
  public void set(String path, Object value) {

  }

  @Override
  public Object get(String path) {
    return null;
  }

  @Override
  public void push(String path, Object value) {
  }

  @Override
  public Object pop(String path) {
    return null;
  }

  @Override
  public void delete(String path) {
  }

  @Override
  public int length(String path) {
    return 0;
  }

  @Override
  public boolean contains(String path, String property) {
    return false;
  }

  @Override
  public void accept(SVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public SMap asMap() {
    return null;
  }

  @Override
  public SList<? extends SNode> asList() {
    return null;
  }

  @Override
  public String asString() {
    return this.interactiveDoc.getDocument().getMutableDoc().toXmlString();
  }

  @Override
  public double asDouble() {
    return 0;
  }

  @Override
  public int asInt() {
    return 0;
  }

  @Override
  public boolean asBoolean() {
    return false;
  }

  @Override
  public SText asText() {
    return this;
  }

  @Override
  public InteractiveDocument getInteractiveDocument() {
    return interactiveDoc;
  }

  @Override
  public ContentDocument getContentDocument() {
    return interactiveDoc.getDocument();
  }

  @Override
  public DocInitialization asDocInitialization() {
    return interactiveDoc.getDocument().asOperation();
  }

  @Override
  public String asXmlString() {
    return this.interactiveDoc.getDocument().getMutableDoc().toXmlString();
  }

  @Override
  public DocHistory getDocHistory() {
    return docHistory;
  }

}
