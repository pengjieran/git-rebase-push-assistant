# EDT Violation Bugfix

## Problem

The plugin was throwing `RuntimeExceptionWithAttachments: Access is allowed from Event Dispatch Thread (EDT) only` when showing error/info dialogs from the background task.

**Root cause**: `Messages.showErrorDialog`, `Messages.showInfoMessage`, and `Messages.showWarningDialog` were being called directly from the `Task.Backgroundable.run()` method, which executes on a background thread. All UI operations in IntelliJ IDEA must run on the Event Dispatch Thread (EDT).

## Solution

Wrapped all `Messages.show*` calls that execute from background threads with `ApplicationManager.getApplication().invokeLater { }` to ensure they run on the EDT.

### Changes in `GitRebaseAndPushAction.kt`

1. **Line 122-127**: Success message after rebase
2. **Line 129-135**: Error message in catch block
3. **Line 147-171**: All three dialog calls in `createMergeRequest()` method

## Pattern

```kotlin
// ❌ Wrong - EDT violation
Messages.showErrorDialog(project, "Error", "Title")

// ✅ Correct - invokeLater ensures EDT execution
com.intellij.openapi.application.ApplicationManager.getApplication()
    .invokeLater {
        Messages.showErrorDialog(project, "Error", "Title")
    }
```

## References

- [IntelliJ Platform Threading](https://jb.gg/ij-platform-threading)
