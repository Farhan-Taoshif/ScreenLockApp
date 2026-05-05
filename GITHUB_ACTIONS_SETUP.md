# GitHub Actions Setup Guide

This guide explains how to set up GitHub Actions for automatic APK building in the ScreenLockApp repository.

## Overview

The ScreenLockApp uses GitHub Actions to automatically build the Android APK whenever code is pushed to the `main` or `master` branch. This allows for continuous integration and easy access to the latest build artifacts.

## Prerequisites

- GitHub account with admin access to the ScreenLockApp repository.
- The repository must be set to allow GitHub Actions.

## Setup Steps

### Step 1: Enable GitHub Actions

1. Go to the [ScreenLockApp repository](https://github.com/Farhan-Taoshif/ScreenLockApp).
2. Click on the **Settings** tab.
3. In the left sidebar, click on **Actions** → **General**.
4. Under **Actions permissions**, select **Allow all actions and reusable workflows**.
5. Click **Save**.

### Step 2: Grant Workflows Permission

Due to GitHub's security model, workflows may need special permissions to create or update files. To enable this:

1. Go to **Settings** → **Actions** → **General**.
2. Scroll down to **Workflow permissions**.
3. Select **Read and write permissions**.
4. Check the box for **Allow GitHub Actions to create and approve pull requests**.
5. Click **Save**.

### Step 3: Add the Workflow File

The workflow file is already included in the repository at `.github/workflows/build.yml`. If it's not present or needs to be recreated:

1. Go to the **Actions** tab in the repository.
2. Click **New workflow** → **set up a workflow yourself**.
3. Name the file `build.yml`.
4. Copy the following content:

```yaml
name: Build APK

on:
  push:
    branches: [ main, master ]
  pull_request:
    branches: [ main, master ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
        with:
          gradle-version: '8.2'

      - name: Grant gradlew permission
        run: chmod +x gradlew || true

      - name: Build Debug APK
        run: gradle assembleDebug

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: ScreenLock-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
          retention-days: 30
```

5. Click **Commit changes**.

### Step 4: Verify the Workflow

1. Go to the **Actions** tab.
2. You should see the **Build APK** workflow listed.
3. Make a test commit or push to trigger the workflow.
4. The workflow should appear in the list and show a green checkmark when successful.

## Accessing Build Artifacts

Once the workflow completes successfully:

1. Go to the **Actions** tab.
2. Click on the latest successful **Build APK** workflow run.
3. Scroll down to the **Artifacts** section.
4. Download the **ScreenLock-debug-apk** artifact (a ZIP file containing the APK).
5. Extract the ZIP file to get the `app-debug.apk`.

## Troubleshooting

### Workflow Not Running

**Issue**: The workflow doesn't run after pushing code.

**Solution**:
- Verify that GitHub Actions is enabled in the repository settings.
- Check that the workflow file is correctly placed at `.github/workflows/build.yml`.
- Ensure you're pushing to the `main` or `master` branch.
- Go to the **Actions** tab and check for any error messages.

### Build Fails

**Issue**: The workflow runs but the build fails.

**Solution**:
- Click on the failed workflow run to see the detailed logs.
- Common issues:
  - Missing Gradle wrapper permissions: The workflow should handle this with `chmod +x gradlew`.
  - Java version mismatch: Ensure JDK 17 is being used.
  - Gradle version mismatch: Ensure Gradle 8.2 is being used.

### Artifacts Not Available

**Issue**: The build succeeds but no artifacts are uploaded.

**Solution**:
- Check that the APK was built at the correct path: `app/build/outputs/apk/debug/app-debug.apk`.
- Verify that the upload step in the workflow is configured correctly.
- Check the workflow logs for any errors during the upload step.

## Manual Workflow Trigger

You can manually trigger the workflow without pushing code:

1. Go to the **Actions** tab.
2. Click on the **Build APK** workflow.
3. Click the **Run workflow** button.
4. Select the branch (usually `main`).
5. Click **Run workflow**.

The workflow will start immediately and build the APK.

## Customizing the Workflow

You can customize the workflow to suit your needs:

- **Change the build type**: Replace `assembleDebug` with `assembleRelease` to build a release APK.
- **Change the retention period**: Modify `retention-days: 30` to change how long artifacts are kept.
- **Add additional steps**: Add more steps to the workflow for testing, signing, or deploying the APK.

For more information on GitHub Actions, see the [official documentation](https://docs.github.com/en/actions).
