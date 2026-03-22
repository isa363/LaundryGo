#include <vector>
#include <string>
#include <fstream>
#include <sstream>
#include <cmath>
#include <iostream>

using namespace std;

// CONSTANTS (from Python calibration)
float ALPHA = 0.15;
float THRESHOLD_ON  = 0.014726;
float THRESHOLD_OFF = 0.012799;
float GRAVITY = 9.837195;
int RMS_WINDOW = 100;


// Low-pass filter
class LowPassFilter {
public:
    LowPassFilter(float alpha) : alpha(alpha), initialized(false), y_prev(0.0f) {}

    float update(float x) {
        if (!initialized) {
            y_prev = x;
            initialized = true;
        }
        float y = alpha * x + (1.0f - alpha) * y_prev;
        y_prev = y;
        return y;
    }

private:
    float alpha;
    float y_prev;
    bool initialized;
};


// Machine class
class Machine {
public:
    Machine(string csvPath, float gravity, float alpha,
            float th_on, float th_off, int rmsWindow)
        : csvPath(csvPath),
          GRAVITY(gravity),
          TH_ON(th_on),
          TH_OFF(th_off),
          RMS_WINDOW(rmsWindow),
          lp(alpha),
          inUse(false),
          useCount(0),
          totalCount(0),
          finished(false)
    {}

    // Load and process the CSV
    void runSimulation() {
        ifstream file(csvPath);
        if (!file.is_open()) {
            cerr << "Error opening file: " << csvPath << endl;
            finished = true;
            return;
        }

        string line;
        getline(file, line); // skip header

        while (getline(file, line)) {
            float t, ax, ay, az;
            char comma;
            stringstream ss(line);

            ss >> t >> comma >> ax >> comma >> ay >> comma >> az;

            float a_tot = sqrt(ax*ax + ay*ay + az*az);
            float a_clean = a_tot - GRAVITY;
            float a_smooth = lp.update(a_clean);

            buffer.push_back(a_smooth);
        }
        file.close();

        // RMS + hysteresis
        for (int i = 0; i + RMS_WINDOW < buffer.size(); i++) {
            float rms = computeRMS(i);

            if (!inUse && rms > TH_ON) inUse = true;
            else if (inUse && rms < TH_OFF) inUse = false;

            if (inUse) useCount++;
            totalCount++;
        }
    }   
        
    // Process ONE sample (one tick)
    void step() {
        if (finished) return;

        string line;
        if (!getline(file, line)) {
            finished = true;
            return;
        }

        float t, ax, ay, az;
        char comma;
        stringstream ss(line);
        ss >> t >> comma >> ax >> comma >> ay >> comma >> az;

        // 1. magnitude
        float a_tot = sqrt(ax*ax + ay*ay + az*az);
        // 2. remove gravity
        float a_clean = a_tot - GRAVITY;
        // 3. real-time smoothing
        float a_smooth = lp.update(a_clean);
        // store for RMS
        buffer.push_back(a_smooth);
        // not enough samples yet
        if (buffer.size() < RMS_WINDOW) return;
        // 4. compute RMS
        float rms = computeRMS(buffer.size() - RMS_WINDOW);
        // 5. hysteresis
        if (!inUse && rms > TH_ON) inUse = true;
        else if (inUse && rms < TH_OFF) inUse = false;
        // 6. stats
        if (inUse) useCount++;
        totalCount++;
    }

    bool isFinished() const { return finished; }

    // Print percentage of IN USE
    void printStats() {
        float percentage = (float)useCount / (float)totalCount;
        cout << "Machine [" << csvPath << "] IN USE % = " << percentage << endl;
    }

private:
    string csvPath;
    float GRAVITY;
    float TH_ON;
    float TH_OFF;
    int RMS_WINDOW;

    LowPassFilter lp;
    bool inUse;

    int useCount;
    int totalCount;

    vector<float> buffer;
    bool finished;
    ifstream file;
    string header;


    float computeRMS(int start) {
        float sum = 0;
        for (int i = start; i < start + RMS_WINDOW; i++)
            sum += buffer[i] * buffer[i];
        return sqrt(sum / RMS_WINDOW);
    }
};




int main() {
    Machine m1("data/idle_50Hz.csv", GRAVITY, ALPHA, THRESHOLD_ON, THRESHOLD_OFF, RMS_WINDOW);
    Machine m2("data/running_empty_50Hz.csv", GRAVITY, ALPHA, THRESHOLD_ON, THRESHOLD_OFF, RMS_WINDOW);
    Machine m3("data/fill_wash.csv", GRAVITY, ALPHA, THRESHOLD_ON, THRESHOLD_OFF, RMS_WINDOW);
    Machine m4("data/fill_wash.csv", GRAVITY, ALPHA, THRESHOLD_ON, THRESHOLD_OFF, RMS_WINDOW);


    //1. Run simulation in various machines simultanioslky
    vector<Machine*> machines = { &m1, &m2, &m3, &m4 };
    bool allDone = false;
    while (!allDone) {
        allDone = true;

        for (auto* m : machines) {
            if (!m->isFinished()) {
                m->step();       // process ONE sample
                allDone = false; // keep going until all files end
            }
        }
    }
    // Print final stats
    m1.printStats();
    m2.printStats();
    m3.printStats();
    m4.printStats();


    return 0;
}
