package org.mozilla.android.sync.synchronizer;


public interface RecordsChannelDelegate {
  public void onFlowCompleted(RecordsChannel recordsChannel, long end);
  public void onFlowBeginFailed(RecordsChannel recordsChannel, Exception ex);
  public void onFlowStoreFailed(RecordsChannel recordsChannel, Exception ex);
  public void onFlowFinishFailed(RecordsChannel recordsChannel, Exception ex);
}