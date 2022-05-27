#include <jni.h>
#include "com_hyejin_TAGON_MainActivity.h"
#include <opencv2/opencv.hpp>

using namespace cv;
using namespace std;

extern "C"{

//전역변수 선언

JNIEXPORT void JNICALL
Java_com_hyejin_TAGON_ExtractTextFromCamera_Imageprocessing(
        JNIEnv *env,
        jobject  instance,
        jlong matAddrInput,
        jlong matAddrResult,
        jlong matAddrOriginal,
        jlong addrTextRoi){


    Mat &matInput = *(Mat *)matAddrInput;
    //텍스트 추출 mat
    Mat &matResult = *(Mat *)matAddrResult;
    Mat &matOriginal=*(Mat *)matAddrOriginal;

    //text Roi mat
    Mat &textRoi=*(Mat *)addrTextRoi;


    //원본
    matOriginal=matInput.clone();
    matResult=matInput;

    Mat textResult;

    //빠른 버전으로 resize
    Mat smallimage;
    resize(matInput, matInput, cv::Size(matInput.cols/5, matInput.rows/5), 0, 0, INTER_AREA);
    //cout << "smallimage " << smallimage.rows << " " << smallimage.cols << std::endl;


    // gray scale 변환
    cvtColor(matInput, matInput, COLOR_RGBA2GRAY);
    // 이진화: 밝기대응
    Mat binaryInput;
    threshold(matInput,binaryInput, 0,255, THRESH_OTSU);
    // 원본 잡음제거
    binaryInput=~binaryInput;
    morphologyEx(binaryInput, binaryInput, MORPH_OPEN,5);
    binaryInput=~binaryInput;

    // 텍스트 영역 찾기
    textResult=~binaryInput; // 반전

    // 열림연산: 잡음제거
    morphologyEx(textResult, textResult, MORPH_OPEN,5);

    //커널디자인 세로길게(뷰 반대)
    Mat kelement(3, 3, CV_8UC1, Scalar(0));
    //kelement.at<uchar>(1, 0) = 1;
    kelement.at<uchar>(0, 1) = 1;
    kelement.at<uchar>(1, 1) = 1;
    kelement.at<uchar>(2, 1) = 1;
    dilate(textResult, textResult, kelement, Point(-1, -1), 4);

    // 열림연산, 영역 이어 붙이기 -> 텍스트 영역 만들기
    //dilate(textResult, textResult, Mat(), Point(-1, -1), 2);    //4

    // 라벨 찾기
    Mat labels, stats, centroids;
    int nlabels = cv::connectedComponentsWithStats(textResult,labels, stats, centroids);

    //text roi
    textRoi= Mat(1,nlabels, CV_16UC4, Scalar(0));

    for (int i = 0; i < nlabels; i++)
    {

        if (i < 2)    continue;

        int area = stats.at<int>(i, cv::CC_STAT_AREA);

        if ( area<1600 && area>300) {     // 바코드 제외 1600

            int left = stats.at<int>(i, cv::CC_STAT_LEFT);
            int top = stats.at<int>(i, cv::CC_STAT_TOP);
            int width = stats.at<int>(i, cv::CC_STAT_WIDTH);
            int height = stats.at<int>(i, cv::CC_STAT_HEIGHT);

            //포인터로	접근으로 해보기
            //-- j열의 주소 (nc 개만큼) 가져오기 --//
            Vec4w *data = textRoi.ptr<Vec4w>(0);
            //---각 화소값 분할--//
            //data[i][0] = left;
            //data[i][1] = top;
            //data[i][2] = width;
            //data[i][3]= height;

            //resize 고려해서 좌표값 넣기!
            data[i][0] = left*5;
            data[i][1] = top*5;
            data[i][2] = width*5;
            data[i][3]= height*5;


            // 텍스트 영역 투명하게
            Mat overlay;
            double alpha = 0.4;
            matResult.copyTo(overlay);

            rectangle(overlay, Point(left*5, top*5),
                      Point(left*5 + width*5, top*5 + height*5), Scalar(255, 255, 0), -1);

            // 텍스트 영역 투명 처리된 matResult 리턴
            // 투명
            addWeighted(overlay, alpha, matResult, 1 - alpha, 0, matResult);
            //cv::putText(image, std::to_string(i), cv::Point(left + 20, top + 20),
            //cv::FONT_HERSHEY_SIMPLEX, 1, cv::Scalar(255, 0, 0), 2);

            //textROI 배열에 넣기 : opencv matrix 접근
            //각 roi left top width height 배열
            //textRoi.at<Vec4w>(0,i)[0] = left;
            //textRoi.at<Vec4w>(0, i)[1] = top;
            //textRoi.at<Vec4w>(0, i)[2] = width;
            //textRoi.at<Vec4w>(0, i)[3] = height;

        }


        // 라벨 확인
            //cv::putText(matResult, to_string(textRoi.at<Vec4b>(0, i)[0]), cv::Point(left, top + 5),
                        //cv::FONT_HERSHEY_SIMPLEX, 0.3, cv::Scalar(255, 255, 0), 2);

        }
    }
}


// ExtractFromAlbum
extern "C" {

void find_label(Mat inputImage);

int textleft = 0;
int texttop = 0;
int textwidth = 0;
int textheight = 0;


JNIEXPORT void JNICALL
Java_com_hyejin_TAGON_MainActivity_Imageprocessing(JNIEnv *env, jobject thiz,
                                                   jlong mat_addr_input,
                                                   jlong mat_addr_result) {
    // TODO: implement Imageprocessing()
    Mat &matInput = *(Mat *) mat_addr_input;
    //텍스트 추출 mat
    Mat &matResult = *(Mat *) mat_addr_result;


    Mat textResult;

    // gray scale 변환
    cvtColor(matInput, matInput, COLOR_RGBA2GRAY);
    // 이진화: 밝기대응
    Mat binaryInput;
    threshold(matInput, binaryInput, 0, 255, THRESH_OTSU);
    // 원본 잡음제거
    binaryInput = ~binaryInput;
    morphologyEx(binaryInput, binaryInput, MORPH_OPEN, 5);
    binaryInput = ~binaryInput;

    // 텍스트 영역 찾기
    textResult = ~binaryInput; // 반전

    // 열림연산: 잡음제거
    morphologyEx(textResult, textResult, MORPH_OPEN, 5);

    //--왼쪽 끝 좌표 커널
    Mat element(5, 5, CV_8UC1, Scalar(0));
    element.at<uchar>(2, 0) = 1;
    element.at<uchar>(2, 1) = 1;
    element.at<uchar>(2, 2) = 1;

    // 열림연산, 영역 이어 붙이기 -> 텍스트 영역 만들기
    Mat lResult;
    dilate(textResult, lResult, element, Point(-1, -1), 48); //48

    // 라벨 찾기
    find_label(lResult);
    int lleft = textleft;
    int ltop = texttop;

    //-- 오른쪽 끝 좌표 커널
    Mat relement(5, 5, CV_8UC1, Scalar(0));
    relement.at<uchar>(2, 4) = 1;
    relement.at<uchar>(2, 3) = 1;
    relement.at<uchar>(2, 2) = 1;

    // 열림연산, 영역 이어 붙이기 -> 텍스트 영역 만들기
    Mat rtextResult;
    dilate(textResult, rtextResult, relement, Point(-1, -1), 48); //48

    // 라벨 찾기
    find_label(rtextResult);

    Rect rect(lleft-4  , ltop-4, textleft + textwidth - lleft+8, texttop + textheight - ltop+8);
    matResult = matInput(rect);
}

void find_label(Mat inputImage) {

    Mat labels, stats, centroids;
    int numOfLables = connectedComponentsWithStats(inputImage, labels, stats, centroids, 8, CV_32S);

    float max;
    float *arr = new float[numOfLables];
    for (int j = 1; j < numOfLables; j++) {
        int area = stats.at<int>(j, CC_STAT_AREA);
        arr[j - 1] = area;

    }


    // 영역중 최대값 구하기
    max = arr[0];
    int maxlabel = 1;
    for (int i = 0; i < numOfLables; i++) {
        if (arr[i] > max) {
            max = arr[i];
            maxlabel = i + 1;
        }

    }

    textleft = stats.at<int>(maxlabel, CC_STAT_LEFT);
    texttop = stats.at<int>(maxlabel, CC_STAT_TOP);
    textwidth = stats.at<int>(maxlabel, CC_STAT_WIDTH);
    textheight = stats.at<int>(maxlabel, CC_STAT_HEIGHT);

}
}