#include <iostream>
#include "android/app/src/main/cpp/pure_cpp_core/include/QtCore/QUuid"
#include "android/app/src/main/cpp/pure_cpp_core/include/QtCore/QString"

int main() {
    QUuid uuid = QUuid::createUuid();
    QString hex = uuid.toRfc4122().toHex();
    std::cout << "Hex: " << hex.toStdString() << std::endl;
    return 0;
}
