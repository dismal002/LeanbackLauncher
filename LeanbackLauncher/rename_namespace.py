import os

def replace_in_file(filepath, old_str, new_str):
    try:
        with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
        if old_str in content:
            new_content = content.replace(old_str, new_str)
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"Updated: {filepath}")
    except Exception as e:
        print(f"Error processing {filepath}: {e}")

def main():
    old_ns = "com.google.android.leanbacklauncher"
    new_ns = "com.dismal.android.leanbacklauncher"
    
    root_dir = "/home/dismal/Documents/TV-Launcher/LeanbackLauncher"
    
    # 1. Update all files in app/src/main/
    src_main = os.path.join(root_dir, "app/src/main")
    for root, dirs, files in os.walk(src_main):
        for file in files:
            if file.endswith(('.java', '.xml')):
                filepath = os.path.join(root, file)
                replace_in_file(filepath, old_ns, new_ns)
                
    # 2. Update app/build.gradle
    build_gradle = os.path.join(root_dir, "app/build.gradle")
    replace_in_file(build_gradle, old_ns, new_ns)
    
    print("Namespace replacement complete!")

if __name__ == "__main__":
    main()
