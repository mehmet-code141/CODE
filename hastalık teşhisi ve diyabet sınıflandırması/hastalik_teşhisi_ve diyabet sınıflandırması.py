# -*- coding: utf-8 -*-
"""
Created on Fri Jan 30 23:44:59 2026

@author: lenovo
"""

# app_streamlit.py
import streamlit as st
import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split, StratifiedKFold, cross_val_score
from sklearn.impute import SimpleImputer
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.metrics import roc_auc_score, roc_curve, classification_report, confusion_matrix
from imblearn.over_sampling import SMOTE
import matplotlib.pyplot as plt
import seaborn as sns

st.set_page_config(page_title='Diyabet Tahmin Uygulaması', layout='centered')

st.title('Diyabet Tahmin Uygulaması')
st.write('Okul projesi için geliştirilmiş, kullanıcı dostu arayüz.')

# --- Yan panel: ayarlar ve örnekler
st.sidebar.header('Ayarlar ve Örnekler')
threshold = st.sidebar.slider('Pozitif sınıf eşiği', 0.0, 1.0, 0.5, 0.01)
use_smote = st.sidebar.checkbox('SMOTE ile dengele', value=True)
retrain_if_upload = st.sidebar.checkbox('CSV yüklendiğinde modeli yeniden eğit', value=True)

st.sidebar.markdown('**Örnek profiller**')
if st.sidebar.button('Ortalama hasta'):
    preset = {'Pregnancies':1, 'Glucose':120, 'BloodPressure':70, 'SkinThickness':20, 'Insulin':79, 'BMI':25.0, 'DiabetesPedigreeFunction':0.5, 'Age':33}
    st.session_state['preset'] = preset
if st.sidebar.button('Yüksek riskli hasta'):
    preset = {'Pregnancies':4, 'Glucose':180, 'BloodPressure':90, 'SkinThickness':35, 'Insulin':200, 'BMI':36.0, 'DiabetesPedigreeFunction':1.2, 'Age':50}
    st.session_state['preset'] = preset
if st.sidebar.button('Düşük riskli genç'):
    preset = {'Pregnancies':0, 'Glucose':85, 'BloodPressure':60, 'SkinThickness':18, 'Insulin':30, 'BMI':21.0, 'DiabetesPedigreeFunction':0.2, 'Age':22}
    st.session_state['preset'] = preset

# --- Kullanıcı girdileri
st.subheader('Hasta bilgileri')
cols = st.columns(2)
with cols[0]:
    pregnancies = st.number_input('Pregnancies', min_value=0, max_value=20, value=st.session_state.get('preset',{}).get('Pregnancies',1))
    glucose = st.number_input('Glucose', min_value=1.0, max_value=300.0, value=st.session_state.get('preset',{}).get('Glucose',120.0))
    bp = st.number_input('BloodPressure', min_value=1.0, max_value=200.0, value=st.session_state.get('preset',{}).get('BloodPressure',70.0))
    skin = st.number_input('SkinThickness', min_value=0.0, max_value=100.0, value=st.session_state.get('preset',{}).get('SkinThickness',20.0))
with cols[1]:
    insulin = st.number_input('Insulin', min_value=0.0, max_value=1000.0, value=st.session_state.get('preset',{}).get('Insulin',79.0))
    bmi = st.number_input('BMI', min_value=5.0, max_value=80.0, value=st.session_state.get('preset',{}).get('BMI',25.0))
    dpf = st.number_input('DiabetesPedigreeFunction', min_value=0.0, max_value=5.0, value=st.session_state.get('preset',{}).get('DiabetesPedigreeFunction',0.5))
    age = st.number_input('Age', min_value=1, max_value=120, value=st.session_state.get('preset',{}).get('Age',33))

# Basit doğrulama uyarıları
if glucose < 40 or glucose > 300:
    st.warning('Glucose değeri olağandışı. Lütfen ölçümü kontrol edin.')
if bmi < 10 or bmi > 60:
    st.warning('BMI değeri olağandışı. Lütfen ölçümü kontrol edin.')

# --- CSV yükleme ile toplu tahmin
st.subheader('Toplu tahmin için CSV yükle')
uploaded = st.file_uploader('CSV dosyası yükleyin. Sütunlar: Pregnancies,Glucose,BloodPressure,SkinThickness,Insulin,BMI,DiabetesPedigreeFunction,Age', type=['csv'])
train_df = None
if uploaded is not None:
    try:
        train_df = pd.read_csv(uploaded)
        st.success(f'Yüklendi: {train_df.shape[0]} satır')
    except Exception as e:
        st.error('CSV okunamadı. Sütunları kontrol edin.')

# --- Model hazırlama fonksiyonu
def build_and_train_model(df=None):
    # Eğer kullanıcı veri yüklediyse onu kullan, yoksa gömülü örnek veri ile hızlı eğitim
    if df is None:
        # Gömülü küçük örnek veri seti (örnek amaçlı)
        data = [
            [6,148,72,35,0,33.6,0.627,50,1],
            [1,85,66,29,0,26.6,0.351,31,0],
            [8,183,64,0,0,23.3,0.672,32,1],
            [1,89,66,23,94,28.1,0.167,21,0],
            [0,137,40,35,168,43.1,2.288,33,1],
            [5,116,74,0,0,25.6,0.201,30,0],
            [3,78,50,32,88,31.0,0.248,26,1],
            [10,115,0,0,0,35.3,0.134,29,0]
        ]
        cols = ['Pregnancies','Glucose','BloodPressure','SkinThickness','Insulin','BMI','DiabetesPedigreeFunction','Age','Outcome']
        df_local = pd.DataFrame(data, columns=cols)
    else:
        df_local = df.copy()
    X = df_local.drop('Outcome', axis=1, errors='ignore')
    y = df_local['Outcome'] if 'Outcome' in df_local.columns else None

    # Eğer yüklenen veri Outcome içeriyorsa eğit, yoksa sadece model eğitmek için örnek kullan
    if y is None:
        # küçük embedded dataset ile eğit
        return build_and_train_model(None)

    # Pipeline
    imputer = SimpleImputer(strategy='median')
    scaler = StandardScaler()
    clf = RandomForestClassifier(n_estimators=100, random_state=42, class_weight='balanced')
    steps = []
    if use_smote:
        steps.append(('smote', SMOTE(random_state=42)))
    steps += [('imputer', imputer), ('scaler', scaler), ('clf', clf)]
    # imblearn Pipeline yerine basit yaklaşım: SMOTE uygulama ayrı
    X_imp = pd.DataFrame(imputer.fit_transform(X), columns=X.columns)
    X_scaled = scaler.fit_transform(X_imp)
    if use_smote:
        X_res, y_res = SMOTE(random_state=42).fit_resample(X_scaled, y)
    else:
        X_res, y_res = X_scaled, y
    clf.fit(X_res, y_res)
    return {'model': clf, 'scaler': scaler, 'imputer': imputer}

# Eğer kullanıcı CSV yüklediyse ve yeniden eğit seçiliyse modeli eğit
model_bundle = None
if train_df is not None and retrain_if_upload:
    with st.spinner('Model eğitiliyor...'):
        model_bundle = build_and_train_model(train_df)
        st.success('Model eğitildi ve kullanılmaya hazır.')

# Eğer model yoksa gömülü örnekle eğit
if model_bundle is None:
    with st.spinner('Hızlı model hazırlanıyor...'):
        model_bundle = build_and_train_model(None)
        st.info('Gömülü örnek veri ile hızlı model oluşturuldu.')

# --- Tekli tahmin butonu
if st.button('Tahmin Et'):
    X_new = pd.DataFrame([[pregnancies, glucose, bp, skin, insulin, bmi, dpf, age]],
                         columns=['Pregnancies','Glucose','BloodPressure','SkinThickness','Insulin','BMI','DiabetesPedigreeFunction','Age'])
    # Impute ve scale
    X_imp = model_bundle['imputer'].transform(X_new)
    X_scaled = model_bundle['scaler'].transform(X_imp)
    proba = model_bundle['model'].predict_proba(X_scaled)[0,1]
    pred = int(proba >= threshold)
    st.markdown('**Sonuç**')
    st.write(f'**Diyabet olma olasılığı:** {proba:.3f}')
    st.write('**Tahmin:**', 'Pozitif (diyabet olabilir)' if pred==1 else 'Negatif (diyabet yok)')
    # Kısa yorum
    if proba >= 0.8:
        st.warning('Yüksek olasılık. Klinik değerlendirme önerilir.')
    elif proba >= 0.5:
        st.info('Orta olasılık. Takip ve ek testler düşünülebilir.')
    else:
        st.success('Düşük olasılık.')

# --- Toplu tahmin
if train_df is not None:
    if st.button('CSV ile toplu tahmin yap'):
        df_in = train_df.copy()
        # Basit doğrulama sütunları
        required = ['Pregnancies','Glucose','BloodPressure','SkinThickness','Insulin','BMI','DiabetesPedigreeFunction','Age']
        missing = [c for c in required if c not in df_in.columns]
        if missing:
            st.error('CSV eksik sütunlar: ' + ', '.join(missing))
        else:
            X_in = df_in[required]
            X_imp = model_bundle['imputer'].transform(X_in)
            X_scaled = model_bundle['scaler'].transform(X_imp)
            probs = model_bundle['model'].predict_proba(X_scaled)[:,1]
            preds = (probs >= threshold).astype(int)
            out = df_in.copy()
            out['Pred_Prob'] = probs
            out['Pred_Label'] = preds
            st.write(out.head(20))
            st.success(f'Toplu tahmin tamamlandı. Toplam satır: {len(out)}')

# --- Basit model değerlendirme görselleştirme
st.subheader('Model değerlendirme örneği')
# Hızlı test: gömülü küçük veri ile ROC çiz
try:
    # oluşturulan model ile gömülü veri kullanılarak örnek ROC
    sample = pd.DataFrame([
        [6,148,72,35,0,33.6,0.627,50,1],
        [1,85,66,29,0,26.6,0.351,31,0],
        [8,183,64,0,0,23.3,0.672,32,1],
        [1,89,66,23,94,28.1,0.167,21,0],
        [0,137,40,35,168,43.1,2.288,33,1],
        [5,116,74,0,0,25.6,0.201,30,0],
        [3,78,50,32,88,31.0,0.248,26,1],
        [10,115,0,0,0,35.3,0.134,29,0]
    ], columns=['Pregnancies','Glucose','BloodPressure','SkinThickness','Insulin','BMI','DiabetesPedigreeFunction','Age','Outcome'])
    Xs = sample.drop('Outcome', axis=1)
    X_imp = model_bundle['imputer'].transform(Xs)
    X_scaled = model_bundle['scaler'].transform(X_imp)
    probs = model_bundle['model'].predict_proba(X_scaled)[:,1]
    auc = roc_auc_score(sample['Outcome'], probs)
    fpr, tpr, _ = roc_curve(sample['Outcome'], probs)
    fig, ax = plt.subplots()
    ax.plot(fpr, tpr, label=f'ROC AUC = {auc:.3f}')
    ax.plot([0,1],[0,1],'k--')
    ax.set_xlabel('False Positive Rate')
    ax.set_ylabel('True Positive Rate')
    ax.legend()
    st.pyplot(fig)
except Exception as e:
    st.info('Değerlendirme görselleştirmesi yapılamadı.')

st.markdown('---')
st.caption('Not: Bu uygulama eğitim amaçlıdır. Klinik karar yerine geçmez.')