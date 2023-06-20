load("github.com/SonarSource/cirrus-modules@v2", "load_features")

def main(ctx):
    return load_features(ctx, aws=dict(zone=2))
