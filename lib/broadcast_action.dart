enum BroadcastActionName {
  BOOT_COMPLETED,
}

class BroadcastAction {
  void Function() callback;
  BroadcastActionName name;

  BroadcastAction(this.name, this.callback);
}
