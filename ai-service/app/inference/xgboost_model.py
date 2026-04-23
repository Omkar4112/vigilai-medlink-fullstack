import os
import pickle
import numpy as np
import logging

logger = logging.getLogger("vigilai-ai")

BASE_DIR   = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MODEL_PATH = os.path.join(BASE_DIR, "models", "xgboost_model.pkl")

_model_instance = None


class XGBoostModel:
    def __init__(self):
        if not os.path.exists(MODEL_PATH):
            raise FileNotFoundError(f"Model not found: {MODEL_PATH}")
        with open(MODEL_PATH, "rb") as f:
            self.model = pickle.load(f)
        logger.info("✅ XGBoost model loaded")

    def predict(self, features: list) -> tuple[float, int]:
        data = np.array(features, dtype=float).reshape(1, -1)
        prob = float(self.model.predict_proba(data)[0][1])
        return prob, int(prob > 0.5)


def get_model() -> XGBoostModel:
    global _model_instance
    if _model_instance is None:
        _model_instance = XGBoostModel()
    return _model_instance


# ── Training script (run once to produce models/xgboost_model.pkl) ────────
# python -c "from app.inference.xgboost_model import train; train()"
def train():
    import pandas as pd
    from xgboost import XGBClassifier
    from sklearn.model_selection import train_test_split
    from sklearn.metrics import classification_report

    DATASET = os.path.join(BASE_DIR, "data", "vitals_dataset.csv")
    if not os.path.exists(DATASET):
        print("Generating synthetic dataset for training …")
        _generate_synthetic(DATASET)

    df = pd.read_csv(DATASET)
    features = ["age", "heart_rate", "spo2", "respiratory_rate",
                "systolic_bp", "diastolic_bp", "temperature"]
    X = df[features]
    y = df["sepsis_label"]

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y)

    model = XGBClassifier(
        n_estimators=200, max_depth=6, learning_rate=0.05,
        subsample=0.8, colsample_bytree=0.8,
        use_label_encoder=False, eval_metric="logloss",
        scale_pos_weight=(y_train == 0).sum() / (y_train == 1).sum()
    )
    model.fit(X_train, y_train,
              eval_set=[(X_test, y_test)], verbose=False)

    y_pred = model.predict(X_test)
    print(classification_report(y_test, y_pred))

    os.makedirs(os.path.join(BASE_DIR, "models"), exist_ok=True)
    with open(MODEL_PATH, "wb") as f:
        pickle.dump(model, f)
    print(f"✅ Model saved → {MODEL_PATH}")


def _generate_synthetic(path: str):
    """Generate synthetic vitals dataset for demo training."""
    import pandas as pd, numpy as np
    rng = np.random.default_rng(42)
    n   = 10_000

    # Normal patients
    normal = pd.DataFrame({
        "age":              rng.integers(18, 85, n//2),
        "heart_rate":       rng.normal(75, 12, n//2).clip(55, 95),
        "spo2":             rng.normal(97, 1.5, n//2).clip(94, 100),
        "respiratory_rate": rng.normal(14, 2, n//2).clip(10, 20),
        "systolic_bp":      rng.normal(120, 15, n//2).clip(100, 150),
        "diastolic_bp":     rng.normal(80, 10, n//2).clip(60, 100),
        "temperature":      rng.normal(36.8, 0.3, n//2).clip(36, 37.5),
        "sepsis_label":     0,
    })

    # Sepsis patients
    septic = pd.DataFrame({
        "age":              rng.integers(18, 85, n//2),
        "heart_rate":       rng.normal(115, 20, n//2).clip(90, 180),
        "spo2":             rng.normal(88, 4, n//2).clip(70, 93),
        "respiratory_rate": rng.normal(28, 5, n//2).clip(22, 50),
        "systolic_bp":      rng.normal(90, 15, n//2).clip(60, 105),
        "diastolic_bp":     rng.normal(60, 12, n//2).clip(35, 80),
        "temperature":      rng.normal(39.1, 0.8, n//2).clip(35, 42),
        "sepsis_label":     1,
    })

    df = pd.concat([normal, septic]).sample(frac=1, random_state=42).reset_index(drop=True)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    df.to_csv(path, index=False)
    print(f"Synthetic dataset → {path} ({len(df)} rows)")
