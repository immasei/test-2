# Sketch-Based Hairstyle Image Retrieval (SBIR)

## Table of Contents

- [Sketch-Based Hairstyle Image Retrieval (SBIR)](#sketch-based-hairstyle-image-retrieval-sbir)
- [1. Set up Environment](#1-set-up-environment)
- [2. Run the program](#2-run-the-program)
  - [2.1 Backend (FastAPI)](#21-backend-fastapi)
  - [2.2 Frontend (React)](#22-frontend-react)
  - [2.3 Access the web application](#23-access-the-web-application)
- [3. Functionalities](#3-functionalities)
  - [3.1 Right Panel: Sketch Canvas & Toolbar](#31-right-panel-sketch-canvas--toolbar)
  - [3.2 Left Panel: Search Results](#32-left-panel-search-results)
- [4. Performance](#4-performance)

## 1. Set up Environment

- Make a new folder (ie `myfolder/`)

1. Download the following:
   - `SBIR.zip` from **Canvas > Project > Proposal - 15**
   - [`archive.zip`](https://www.kaggle.com/datasets/gautamvari/sketchhairsalon) (hairstyle dataset)

2. Place both `.zip` files in a folder (e.g., `myfolder/`)

3. Unzip the files

```bash
  unzip SBIR.zip     # creates SBIR/
  unzip archive.zip  # creates dataset/
```

4. Current directory structure

  ```
    myfolder/ (you should be here)
      ├── dataset/
      │   └── unbraid/
      │       └── img/
      │           ├── test/
      │           └── train/
      └── SBIR/
  ```

5. Combine the dataset

```bash
  mkdir -p dataset/unbraid/img/all
```

```bash
  mv dataset/unbraid/img/test/* dataset/unbraid/img/train/* dataset/unbraid/img/all/
```

6. Current directory structure

  ```
    myfolder/ (you are here)
      ├── dataset/
      │   └── unbraid/
      │       └── img/
      │           ├── test/
      │           ├── train/
      │           └── all/
      └── SBIR/
  ```

## 2. Run the program

> Open two terminals — one for the backend and one for the frontend.

### 2.1 Backend (FastAPI)

- http://localhost:8000

```bash
  cd SBIR/backend
  pip install -r requirements.txt
  python3 -m uvicorn main:app --reload --port 8000
```

> First-time model loading may take ~5 minutes.

### 2.2 Frontend (React)

- http://localhost:3000

```bash
  cd SBIR/frontend
  npm install
  npm run dev
```

### 2.3 Access the web application

- Open your browser and navigate to:
 
  ```
    http://localhost:3000
  ```

## 3. Functionalities

### 3.1 Right Panel: Sketch Canvas & Toolbar

- User can sketch on the right panel

- Brush mode: Default drawing mode

- Eraser mode: Remove sketch strokes

- Clear: Erase entire canvas and return to brush mode

- Color palette: Choose brush color and auto-return to brush mode

- Search: Sends sketch to backend, retrieves top 10 similar hairstyles

- Download: Downloads current sketch + results

### 3.2 Left Panel: Search Results

- Displays top 10 similar hairstyles

- Click on a result to view more details

## 4. Performance

- Current performance: best hits @1, 10 : 68, 96
